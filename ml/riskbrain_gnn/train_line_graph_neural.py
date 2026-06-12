import argparse
import csv
import json
import math
import os
from collections import defaultdict, deque
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Tuple

import torch
import torch.nn as nn
import torch.nn.functional as F


RELATION_NAMES = (
    "sender_recent_in",
    "sender_recent_out",
    "receiver_recent_in",
    "receiver_recent_out",
    "same_pair",
    "reverse_pair",
)
RELATION_TO_ID = {name: index for index, name in enumerate(RELATION_NAMES)}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train a temporal-past line graph neural model for iBank RiskBrain.")
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--split-file", required=True)
    parser.add_argument("--temporal-feature-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--model-version", default="riskbrain-linegraph-neural-v8")
    parser.add_argument("--feature-version", default="riskgraph-linegraph-neural-v8")
    parser.add_argument("--epochs", type=int, default=35)
    parser.add_argument("--hidden-dim", type=int, default=128)
    parser.add_argument("--dropout", type=float, default=0.20)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-4)
    parser.add_argument("--focal-gamma", type=float, default=2.0)
    parser.add_argument("--positive-weight-cap", type=float, default=50.0)
    parser.add_argument("--max-neighbors-per-context", type=int, default=3)
    parser.add_argument("--max-time-gap-hours", type=float, default=24 * 7)
    parser.add_argument("--architecture", choices=["temporal_past_sage", "relation_temporal_past_sage"],
                        default="temporal_past_sage")
    parser.add_argument("--review-threshold", type=float, default=None)
    parser.add_argument("--block-threshold", type=float, default=0.90)
    parser.add_argument("--seed", type=int, default=20260530)
    parser.add_argument("--device", default="cuda" if torch.cuda.is_available() else "cpu")
    return parser.parse_args()


def read_csv(path: str) -> Iterable[Dict[str, str]]:
    with open(path, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            yield row


def as_int(value, default: int = 0) -> int:
    if value is None or value == "":
        return default
    try:
        return int(float(value))
    except ValueError:
        return default


def as_float(value, default: float = 0.0) -> float:
    if value is None or value == "":
        return default
    try:
        return float(value)
    except ValueError:
        return default


def parse_event_order(row: Dict[str, str], fallback_index: int) -> float:
    value = row.get("event_time", "")
    if value:
        text = value.strip()
        for fmt in ("%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d %H:%M:%S"):
            try:
                return datetime.strptime(text, fmt).timestamp()
            except ValueError:
                pass
    source_step = row.get("source_step")
    if source_step:
        return as_float(source_step, float(fallback_index))
    source_row_no = row.get("source_row_no")
    if source_row_no:
        return as_float(source_row_no, float(fallback_index))
    return float(fallback_index)


def load_node_ids(data_dir: str) -> set:
    return {as_int(row.get("node_id"), -1) for row in read_csv(os.path.join(data_dir, "nodes.csv"))}


def load_edges(data_dir: str) -> List[Dict]:
    node_ids = load_node_ids(data_dir)
    rows = []
    for row_index, row in enumerate(read_csv(os.path.join(data_dir, "edges.csv"))):
        if as_int(row.get("from_node_id"), -1) not in node_ids:
            continue
        if as_int(row.get("to_node_id"), -1) not in node_ids:
            continue
        rows.append({
            "model_index": len(rows),
            "row_index": row_index,
            "event_order": parse_event_order(row, row_index),
            "row": row,
        })
    return rows


def load_split(split_file: str) -> Dict[int, Dict]:
    mapping = {}
    with open(split_file, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        required = {"model_index", "split", "order_rank", "event_order"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"split file missing required columns: {sorted(missing)}")
        for row in reader:
            model_index = as_int(row.get("model_index"), -1)
            mapping[model_index] = {
                "split": row.get("split", ""),
                "order_rank": as_int(row.get("order_rank"), model_index),
                "event_order": as_float(row.get("event_order"), 0.0),
            }
    return mapping


def load_temporal_features(path: str) -> Tuple[torch.Tensor, torch.Tensor, List[str]]:
    rows = []
    labels = []
    feature_columns: List[str] = []
    with open(path, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        feature_columns = [column for column in (reader.fieldnames or []) if column.startswith("f")]
        if not feature_columns:
            raise ValueError("temporal feature file has no fNNN columns")
        for row in reader:
            rows.append([as_float(row.get(column), 0.0) for column in feature_columns])
            labels.append(1 if as_int(row.get("label_fraud"), 0) == 1 else 0)
    return torch.tensor(rows, dtype=torch.float32), torch.tensor(labels, dtype=torch.float32), feature_columns


def split_ids(split_map: Dict[int, Dict], split: str) -> torch.Tensor:
    values = [model_index for model_index, meta in split_map.items() if meta["split"] == split]
    return torch.tensor(sorted(values), dtype=torch.long)


def fit_standardizer(x: torch.Tensor, ids: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
    scoped = x[ids]
    mean = scoped.mean(dim=0, keepdim=True)
    std = scoped.std(dim=0, keepdim=True).clamp_min(1e-6)
    return mean, std


def tensor_stats_to_json(mean: torch.Tensor, std: torch.Tensor) -> Dict[str, List[float]]:
    return {
        "mean": [float(value) for value in mean.squeeze(0).tolist()],
        "std": [float(value) for value in std.squeeze(0).tolist()],
    }


def recent_items(queue: deque, current_time: float, max_gap_seconds: float, limit: int) -> List[int]:
    cutoff = current_time - max_gap_seconds
    while queue and queue[0][0] < cutoff:
        queue.popleft()
    return [model_index for _, model_index in list(queue)[-limit:]]


def build_line_graph_edges(edges: List[Dict], split_map: Dict[int, Dict],
                           max_neighbors: int, max_gap_hours: float) -> Tuple[torch.Tensor, torch.Tensor, Dict]:
    incoming_by_node = defaultdict(deque)
    outgoing_by_node = defaultdict(deque)
    pair_history = defaultdict(deque)
    sources = []
    targets = []
    relation_ids = []
    relation_counts = defaultdict(int)
    max_gap_seconds = max_gap_hours * 3600.0
    ordered = sorted(edges, key=lambda item: (split_map[item["model_index"]]["order_rank"], item["row_index"]))

    def add_context(context: List[int], current: int, relation: str, seen: set) -> None:
        for prior in context:
            if prior == current or (prior, current) in seen:
                continue
            seen.add((prior, current))
            sources.append(prior)
            targets.append(current)
            relation_ids.append(RELATION_TO_ID[relation])
            relation_counts[relation] += 1

    for item in ordered:
        current = item["model_index"]
        row = item["row"]
        current_time = split_map[current].get("event_order") or item["event_order"]
        src_id = as_int(row.get("from_node_id"), -1)
        dst_id = as_int(row.get("to_node_id"), -1)
        pair_key = (src_id, dst_id)
        reverse_key = (dst_id, src_id)
        seen = set()
        add_context(recent_items(incoming_by_node[src_id], current_time, max_gap_seconds, max_neighbors),
                    current, "sender_recent_in", seen)
        add_context(recent_items(outgoing_by_node[src_id], current_time, max_gap_seconds, max_neighbors),
                    current, "sender_recent_out", seen)
        add_context(recent_items(incoming_by_node[dst_id], current_time, max_gap_seconds, max_neighbors),
                    current, "receiver_recent_in", seen)
        add_context(recent_items(outgoing_by_node[dst_id], current_time, max_gap_seconds, max_neighbors),
                    current, "receiver_recent_out", seen)
        add_context(recent_items(pair_history[pair_key], current_time, max_gap_seconds, max_neighbors),
                    current, "same_pair", seen)
        add_context(recent_items(pair_history[reverse_key], current_time, max_gap_seconds, max_neighbors),
                    current, "reverse_pair", seen)

        incoming_by_node[dst_id].append((current_time, current))
        outgoing_by_node[src_id].append((current_time, current))
        pair_history[pair_key].append((current_time, current))

    edge_index = torch.tensor([sources, targets], dtype=torch.long)
    relation_id_tensor = torch.tensor(relation_ids, dtype=torch.long)
    meta = {
        "line_graph_edges": int(edge_index.size(1)),
        "relation_names": list(RELATION_NAMES),
        "relation_counts": {name: int(relation_counts.get(name, 0)) for name in RELATION_NAMES},
        "max_neighbors_per_context": max_neighbors,
        "max_time_gap_hours": max_gap_hours,
        "adjacency_scope": "temporal_past",
    }
    return edge_index, relation_id_tensor, meta


def build_temporal_past_adj(edge_index: torch.Tensor, num_nodes: int, device: torch.device) -> torch.Tensor:
    src = edge_index[0]
    dst = edge_index[1]
    self_nodes = torch.arange(num_nodes, dtype=torch.long)
    row = torch.cat([dst, self_nodes])
    col = torch.cat([src, self_nodes])
    values = torch.ones(row.numel(), dtype=torch.float32)
    degree = torch.zeros(num_nodes, dtype=torch.float32)
    degree.scatter_add_(0, row, values)
    norm_values = values / degree[row].clamp_min(1.0)
    indices = torch.stack([row, col], dim=0)
    return torch.sparse_coo_tensor(indices, norm_values, (num_nodes, num_nodes)).coalesce().to(device)


def build_relation_temporal_past_adjs(edge_index: torch.Tensor, relation_ids: torch.Tensor,
                                      relation_count: int, num_nodes: int,
                                      device: torch.device) -> List[torch.Tensor]:
    adjs = []
    for relation_id in range(relation_count):
        mask = relation_ids == relation_id
        rel_edges = edge_index[:, mask]
        if rel_edges.numel() == 0:
            empty_indices = torch.empty((2, 0), dtype=torch.long, device=device)
            empty_values = torch.empty((0,), dtype=torch.float32, device=device)
            adjs.append(torch.sparse_coo_tensor(empty_indices, empty_values, (num_nodes, num_nodes)).coalesce())
            continue
        src = rel_edges[0]
        dst = rel_edges[1]
        row = dst
        col = src
        values = torch.ones(row.numel(), dtype=torch.float32)
        degree = torch.zeros(num_nodes, dtype=torch.float32)
        degree.scatter_add_(0, row, values)
        norm_values = values / degree[row].clamp_min(1.0)
        indices = torch.stack([row, col], dim=0)
        adjs.append(torch.sparse_coo_tensor(indices, norm_values, (num_nodes, num_nodes)).coalesce().to(device))
    return adjs


class TemporalPastSageLayer(nn.Module):
    def __init__(self, in_dim: int, out_dim: int):
        super().__init__()
        self.linear = nn.Linear(in_dim * 2, out_dim)

    def forward(self, x: torch.Tensor, adj: torch.Tensor) -> torch.Tensor:
        incoming = torch.sparse.mm(adj, x)
        return self.linear(torch.cat([x, incoming], dim=-1))


class LineGraphRiskModel(nn.Module):
    def __init__(self, feature_dim: int, hidden_dim: int, dropout: float):
        super().__init__()
        self.input = nn.Linear(feature_dim, hidden_dim)
        self.sage1 = TemporalPastSageLayer(hidden_dim, hidden_dim)
        self.sage2 = TemporalPastSageLayer(hidden_dim, hidden_dim)
        self.dropout = nn.Dropout(dropout)
        self.head = nn.Sequential(
            nn.Linear(hidden_dim + feature_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim, 1),
        )

    def forward(self, x: torch.Tensor, adj: torch.Tensor) -> torch.Tensor:
        h = F.relu(self.input(x))
        h = self.dropout(h)
        h = F.relu(self.sage1(h, adj))
        h = self.dropout(h)
        h = F.relu(self.sage2(h, adj))
        return self.head(torch.cat([h, x], dim=-1)).squeeze(-1)


class RelationTemporalPastSageLayer(nn.Module):
    def __init__(self, in_dim: int, out_dim: int, relation_count: int, dropout: float):
        super().__init__()
        self.root = nn.Linear(in_dim, out_dim)
        self.relation_linears = nn.ModuleList([nn.Linear(in_dim, out_dim) for _ in range(relation_count)])
        self.relation_gates = nn.Parameter(torch.zeros(relation_count))
        self.norm = nn.LayerNorm(out_dim)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x: torch.Tensor, relation_adjs: List[torch.Tensor]) -> torch.Tensor:
        out = self.root(x)
        gates = torch.sigmoid(self.relation_gates)
        for relation_id, adj in enumerate(relation_adjs):
            if adj._nnz() == 0:
                continue
            incoming = torch.sparse.mm(adj, x)
            out = out + gates[relation_id] * self.relation_linears[relation_id](incoming)
        return self.norm(self.dropout(out))


class RelationLineGraphRiskModel(nn.Module):
    def __init__(self, feature_dim: int, hidden_dim: int, relation_count: int, dropout: float):
        super().__init__()
        self.input = nn.Sequential(
            nn.Linear(feature_dim, hidden_dim),
            nn.LayerNorm(hidden_dim),
            nn.ReLU(),
        )
        self.layer1 = RelationTemporalPastSageLayer(hidden_dim, hidden_dim, relation_count, dropout)
        self.layer2 = RelationTemporalPastSageLayer(hidden_dim, hidden_dim, relation_count, dropout)
        self.dropout = nn.Dropout(dropout)
        self.head = nn.Sequential(
            nn.Linear(hidden_dim + feature_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim // 2, 1),
        )

    def forward(self, x: torch.Tensor, relation_adjs: List[torch.Tensor]) -> torch.Tensor:
        h0 = self.input(x)
        h1 = F.relu(self.layer1(h0, relation_adjs))
        h1 = self.dropout(h1)
        h2 = F.relu(self.layer2(h1, relation_adjs) + h1)
        return self.head(torch.cat([h2, x], dim=-1)).squeeze(-1)


def binary_auc(scores: torch.Tensor, labels: torch.Tensor) -> float:
    labels = labels.float()
    pos = labels.sum().item()
    neg = labels.numel() - pos
    if pos == 0 or neg == 0:
        return float("nan")
    order = torch.argsort(scores)
    ranks = torch.empty_like(order, dtype=torch.float32)
    ranks[order] = torch.arange(1, labels.numel() + 1, device=scores.device, dtype=torch.float32)
    pos_rank_sum = ranks[labels == 1].sum().item()
    return (pos_rank_sum - pos * (pos + 1) / 2.0) / (pos * neg)


def average_precision(scores: torch.Tensor, labels: torch.Tensor) -> float:
    labels = labels.float()
    pos = labels.sum().item()
    if pos == 0:
        return float("nan")
    order = torch.argsort(scores, descending=True)
    sorted_labels = labels[order]
    tp = torch.cumsum(sorted_labels, dim=0)
    rank = torch.arange(1, sorted_labels.numel() + 1, device=scores.device, dtype=torch.float32)
    precision = tp / rank
    return float((precision * sorted_labels).sum().item() / pos)


def metrics_for_probs(probs: torch.Tensor, labels: torch.Tensor, threshold: float = None) -> Dict[str, float]:
    probs = probs.detach()
    labels = labels.detach().float()
    if threshold is None:
        order = torch.argsort(probs, descending=True)
        sorted_probs = probs[order]
        sorted_labels = labels[order]
        tp_curve = torch.cumsum(sorted_labels, dim=0)
        rank = torch.arange(1, sorted_labels.numel() + 1, device=probs.device, dtype=torch.float32)
        precision_curve = tp_curve / rank
        recall_curve = tp_curve / max(1.0, labels.sum().item())
        f1_curve = 2 * precision_curve * recall_curve / (precision_curve + recall_curve).clamp_min(1e-12)
        best_idx = int(torch.argmax(f1_curve).item())
        threshold = float(sorted_probs[best_idx].item())
        best_precision = float(precision_curve[best_idx].item())
        best_recall = float(recall_curve[best_idx].item())
        best_f1 = float(f1_curve[best_idx].item())
    else:
        best_precision = 0.0
        best_recall = 0.0
        best_f1 = 0.0
    preds = (probs >= threshold).float()
    tp = int(((preds == 1) & (labels == 1)).sum().item())
    fp = int(((preds == 1) & (labels == 0)).sum().item())
    fn = int(((preds == 0) & (labels == 1)).sum().item())
    tn = int(((preds == 0) & (labels == 0)).sum().item())
    precision = tp / max(1, tp + fp)
    recall = tp / max(1, tp + fn)
    f1 = 2 * precision * recall / max(1e-12, precision + recall)
    result = {
        "threshold": float(threshold),
        "auc": binary_auc(probs, labels),
        "ap": average_precision(probs, labels),
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "tn": tn,
        "positive_edges": int(labels.sum().item()),
        "total_edges": int(labels.numel()),
    }
    if best_f1:
        result.update({
            "best_threshold": float(threshold),
            "best_precision": best_precision,
            "best_recall": best_recall,
            "best_f1": best_f1,
        })
    return result


def focal_loss(logits: torch.Tensor, labels: torch.Tensor, pos_weight: torch.Tensor, gamma: float) -> torch.Tensor:
    bce = F.binary_cross_entropy_with_logits(logits, labels, pos_weight=pos_weight, reduction="none")
    probs = torch.sigmoid(logits)
    p_t = probs * labels + (1.0 - probs) * (1.0 - labels)
    return (bce * torch.pow((1.0 - p_t).clamp_min(1e-6), gamma)).mean()


def decision_for(probability: float, review_threshold: float, block_threshold: float) -> str:
    if probability >= block_threshold:
        return "BLOCK"
    if probability >= review_threshold:
        return "REVIEW"
    return "PASS"


def feature_mode_for(args: argparse.Namespace) -> str:
    if args.architecture == "relation_temporal_past_sage":
        return "line_neural_v2"
    return "line_neural_v1"


def architecture_label_for(args: argparse.Namespace) -> str:
    if args.architecture == "relation_temporal_past_sage":
        return "line_graph_relation_temporal_past_sage"
    return "line_graph_temporal_past_sage"


def write_edge_scores(path: str, edges: List[Dict], probabilities: List[float],
                      args: argparse.Namespace, review_threshold: float) -> Dict[str, int]:
    fields = [
        "graph_edge_id", "batch_code", "dataset_name", "source_row_no", "from_node_id", "to_node_id",
        "edge_type", "amount", "currency", "event_time", "label_fraud", "model_version", "feature_version",
        "risk_probability", "risk_score", "decision", "review_threshold", "block_threshold", "reason_json"
    ]
    decision_counts: Dict[str, int] = {}
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for item, probability in zip(edges, probabilities):
            row = item["row"]
            decision = decision_for(float(probability), review_threshold, args.block_threshold)
            decision_counts[decision] = decision_counts.get(decision, 0) + 1
            reason = {
                "model": args.model_version,
                "feature_version": args.feature_version,
                "architecture": architecture_label_for(args),
                "feature_mode": feature_mode_for(args),
                "adjacency_scope": "temporal_past",
                "threshold_source": "validation_best_f1" if args.review_threshold is None else "cli",
            }
            writer.writerow({
                "graph_edge_id": row.get("edge_id", ""),
                "batch_code": row.get("batch_code", ""),
                "dataset_name": row.get("dataset_name", ""),
                "source_row_no": row.get("source_row_no", ""),
                "from_node_id": row.get("from_node_id", ""),
                "to_node_id": row.get("to_node_id", ""),
                "edge_type": row.get("edge_type", ""),
                "amount": row.get("amount", ""),
                "currency": row.get("currency", ""),
                "event_time": row.get("event_time", ""),
                "label_fraud": row.get("label_fraud", ""),
                "model_version": args.model_version,
                "feature_version": args.feature_version,
                "risk_probability": f"{float(probability):.8f}",
                "risk_score": str(round(float(probability) * 1000)),
                "decision": decision,
                "review_threshold": f"{review_threshold:.8f}",
                "block_threshold": f"{args.block_threshold:.8f}",
                "reason_json": json.dumps(reason, ensure_ascii=False, separators=(",", ":")),
            })
    return decision_counts


def train(args: argparse.Namespace) -> Dict:
    torch.manual_seed(args.seed)
    torch.cuda.manual_seed_all(args.seed)
    os.makedirs(args.output_dir, exist_ok=True)
    device = torch.device(args.device)
    edges = load_edges(args.data_dir)
    split_map = load_split(args.split_file)
    if set(split_map.keys()) != {item["model_index"] for item in edges}:
        raise ValueError("split file assignments do not match eligible edges from data-dir")
    x, y, feature_columns = load_temporal_features(args.temporal_feature_file)
    if x.size(0) != len(edges):
        raise ValueError(f"feature row count does not match edge count: {x.size(0)} vs {len(edges)}")
    train_ids = split_ids(split_map, "train")
    val_ids = split_ids(split_map, "val")
    test_ids = split_ids(split_map, "test")
    mean, std = fit_standardizer(x, train_ids)
    x = (x - mean) / std
    line_edge_index, relation_ids, line_meta = build_line_graph_edges(
        edges, split_map, args.max_neighbors_per_context, args.max_time_gap_hours)

    positives = y[train_ids].sum().item()
    negatives = train_ids.numel() - positives
    pos_weight_value = min(args.positive_weight_cap, negatives / max(1.0, positives))
    pos_weight = torch.tensor([pos_weight_value], device=device)

    x = x.to(device)
    y = y.to(device)
    train_ids = train_ids.to(device)
    val_ids = val_ids.to(device)
    test_ids = test_ids.to(device)
    if args.architecture == "relation_temporal_past_sage":
        relation_adjs = build_relation_temporal_past_adjs(
            line_edge_index, relation_ids, len(RELATION_NAMES), x.size(0), device)
        model = RelationLineGraphRiskModel(x.size(1), args.hidden_dim, len(RELATION_NAMES), args.dropout).to(device)
        graph_input = relation_adjs
    else:
        adj = build_temporal_past_adj(line_edge_index, x.size(0), device)
        model = LineGraphRiskModel(x.size(1), args.hidden_dim, args.dropout).to(device)
        graph_input = adj
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)

    best_payload = None
    best_val_ap = -1.0
    history = []
    meta = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "data_dir": os.path.abspath(args.data_dir),
        "split_file": os.path.abspath(args.split_file),
        "temporal_feature_file": os.path.abspath(args.temporal_feature_file),
        "model_version": args.model_version,
        "feature_version": args.feature_version,
        "feature_mode": feature_mode_for(args),
        "feedback_mode": "off",
        "split_mode": "time",
        "effective_split_mode": "external_temporal",
        "standardize_scope": "train",
        "adjacency_scope": "temporal_past",
        "normalization": {
            "scope": "train",
            "node": tensor_stats_to_json(mean, std),
        },
        "feature_columns": feature_columns,
        "feature_count": len(feature_columns),
        "train_edges": int(train_ids.numel()),
        "val_edges": int(val_ids.numel()),
        "test_edges": int(test_ids.numel()),
        "train_positive_edges": int(y[train_ids].sum().item()),
        "val_positive_edges": int(y[val_ids].sum().item()),
        "test_positive_edges": int(y[test_ids].sum().item()),
        "hidden_dim": args.hidden_dim,
        "epochs": args.epochs,
        "device": str(device),
        "positive_weight": pos_weight_value,
        "architecture": architecture_label_for(args),
    }
    meta.update(line_meta)
    print(json.dumps(meta, indent=2, ensure_ascii=False))

    for epoch in range(1, args.epochs + 1):
        model.train()
        logits = model(x, graph_input)
        loss = focal_loss(logits[train_ids], y[train_ids], pos_weight, args.focal_gamma)
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        optimizer.step()

        model.eval()
        with torch.no_grad():
            val_logits = model(x, graph_input)[val_ids]
            val_probs = torch.sigmoid(val_logits)
            val_metrics = metrics_for_probs(val_probs, y[val_ids])
        record = {
            "epoch": epoch,
            "train_loss": float(loss.item()),
        }
        record.update({"val_" + key: value for key, value in val_metrics.items()})
        history.append(record)
        print(json.dumps(record, ensure_ascii=False))
        if best_payload is None or (not math.isnan(val_metrics["ap"]) and val_metrics["ap"] > best_val_ap):
            best_val_ap = val_metrics["ap"]
            best_payload = {
                "model_state": {key: value.detach().cpu() for key, value in model.state_dict().items()},
                "epoch": epoch,
                "meta": meta,
                "val_metrics": val_metrics,
            }

    model.load_state_dict(best_payload["model_state"])
    model.eval()
    with torch.no_grad():
        logits = model(x, graph_input)
        probabilities = torch.sigmoid(logits)
        val_probs = probabilities[val_ids]
        test_probs = probabilities[test_ids]
        val_metrics = metrics_for_probs(val_probs, y[val_ids])
        review_threshold = args.review_threshold if args.review_threshold is not None else float(val_metrics["best_threshold"])
        test_metrics = metrics_for_probs(test_probs, y[test_ids], review_threshold)
        test_oracle_metrics = metrics_for_probs(test_probs, y[test_ids])
        all_probs = probabilities.detach().cpu().tolist()

    score_path = os.path.join(args.output_dir, "edge_scores.csv")
    decision_counts = write_edge_scores(score_path, edges, all_probs, args, review_threshold)
    score_summary = {
        "output": os.path.abspath(score_path),
        "num_edges": len(all_probs),
        "review_threshold": review_threshold,
        "block_threshold": args.block_threshold,
        "decision_counts": decision_counts,
    }
    with open(score_path + ".summary.json", "w", encoding="utf-8") as handle:
        json.dump(score_summary, handle, indent=2, ensure_ascii=False)

    result = {
        "meta": meta,
        "best_epoch": best_payload["epoch"],
        "best_val_metrics": best_payload["val_metrics"],
        "test_metrics": test_metrics,
        "test_oracle_metrics": test_oracle_metrics,
        "score_summary": score_summary,
        "history": history,
    }
    with open(os.path.join(args.output_dir, "metrics.json"), "w", encoding="utf-8") as handle:
        json.dump(result, handle, indent=2, ensure_ascii=False)
    torch.save(best_payload, os.path.join(args.output_dir, "best_model.pt"))
    return result


def main() -> None:
    args = parse_args()
    result = train(args)
    print("Final line graph neural metrics:")
    print(json.dumps({
        "best_epoch": result["best_epoch"],
        "best_val_metrics": result["best_val_metrics"],
        "test_metrics": result["test_metrics"],
        "test_oracle_metrics": result["test_oracle_metrics"],
        "score_summary": result["score_summary"],
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
