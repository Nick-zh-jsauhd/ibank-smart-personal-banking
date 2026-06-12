import argparse
import csv
import json
import os
from typing import Dict, Iterable, List

import torch

from train_graphsage_edge import apply_normalization_from_meta, build_model_adj, create_model, load_graph


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Score iBank RiskBrain graph edges with a trained GraphSAGE model.")
    parser.add_argument("--data-dir", required=True, help="Directory containing nodes.csv and edges.csv.")
    parser.add_argument("--checkpoint", required=True, help="Path to best_model.pt.")
    parser.add_argument("--output", required=True, help="Output edge score CSV.")
    parser.add_argument("--model-version", default="riskbrain-graphsage-v1")
    parser.add_argument("--feature-version", default="riskgraph-v1")
    parser.add_argument("--batch-size", type=int, default=32768)
    parser.add_argument("--max-edges", type=int, default=0)
    parser.add_argument("--feature-mode", choices=["v1", "v2", "v3", "temporal_v1"], default=None,
                        help="Defaults to the feature mode stored in the checkpoint, or v1 for old checkpoints.")
    parser.add_argument("--temporal-feature-file", default=None,
                        help="Required when scoring a temporal_v1 checkpoint unless the checkpoint path is still valid.")
    parser.add_argument("--feedback-mode", choices=["off", "weight", "label"], default=None,
                        help="Defaults to the feedback mode stored in the checkpoint, or label for old checkpoints.")
    parser.add_argument("--architecture", choices=["graphsage", "directed-edge-sage"], default=None,
                        help="Defaults to the architecture stored in the checkpoint, or graphsage for old checkpoints.")
    parser.add_argument("--review-threshold", type=float, default=None)
    parser.add_argument("--block-threshold", type=float, default=0.90)
    parser.add_argument("--device", default="cuda" if torch.cuda.is_available() else "cpu")
    return parser.parse_args()


def read_csv(path: str) -> Iterable[Dict[str, str]]:
    with open(path, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            yield row


def as_int(value: str) -> int:
    if value is None or value == "":
        return 0
    return int(float(value))


def csv_value(value) -> str:
    if value is None:
        return ""
    text = str(value)
    if "," not in text and "\"" not in text and "\n" not in text and "\r" not in text:
        return text
    return "\"" + text.replace("\"", "\"\"") + "\""


def load_edge_identity(data_dir: str, max_edges: int = 0) -> List[Dict[str, str]]:
    node_ids = set()
    for row in read_csv(os.path.join(data_dir, "nodes.csv")):
        node_ids.add(as_int(row["node_id"]))

    identities = []
    for row in read_csv(os.path.join(data_dir, "edges.csv")):
        if max_edges > 0 and len(identities) >= max_edges:
            break
        if as_int(row["from_node_id"]) not in node_ids or as_int(row["to_node_id"]) not in node_ids:
            continue
        identities.append({
            "graph_edge_id": row["edge_id"],
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
        })
    return identities


def decision_for(probability: float, review_threshold: float, block_threshold: float) -> str:
    if probability >= block_threshold:
        return "BLOCK"
    if probability >= review_threshold:
        return "REVIEW"
    return "PASS"


@torch.no_grad()
def score(args: argparse.Namespace) -> Dict:
    device = torch.device(args.device)
    checkpoint = torch.load(args.checkpoint, map_location=device)
    model_meta = checkpoint.get("meta", {})
    val_metrics = checkpoint.get("val_metrics", {})
    feature_mode = args.feature_mode or model_meta.get("feature_mode", "v1")
    feedback_mode = args.feedback_mode or model_meta.get("feedback_mode", "label")
    architecture = args.architecture or model_meta.get("architecture", "graphsage")
    temporal_feature_file = args.temporal_feature_file or model_meta.get("temporal_feature_file")
    review_threshold = args.review_threshold
    if review_threshold is None:
        review_threshold = float(val_metrics.get("best_threshold", 0.75))

    x, edge_index, edge_attr, _, _, data_meta = load_graph(
        args.data_dir, args.max_edges, feature_mode, feedback_mode, standardize_features=False,
        temporal_feature_file=temporal_feature_file)
    x, edge_attr = apply_normalization_from_meta(x, edge_attr, model_meta)
    identities = load_edge_identity(args.data_dir, args.max_edges)
    if len(identities) != edge_index.size(1):
        raise RuntimeError("Loaded edge identity count does not match graph edge count: "
                           f"{len(identities)} vs {edge_index.size(1)}")

    x = x.to(device)
    edge_index = edge_index.to(device)
    edge_attr = edge_attr.to(device)
    adj = build_model_adj(edge_index.cpu(), x.size(0), device, architecture)

    hidden_dim = int(model_meta.get("hidden_dim", model_meta.get("hidden", 64)))
    model = create_model(architecture, x.size(1), edge_attr.size(1), hidden_dim, dropout=0.0).to(device)
    model.load_state_dict(checkpoint["model_state"])
    model.eval()

    h = model.encode(x, adj)
    probabilities = []
    for start in range(0, edge_index.size(1), args.batch_size):
        end = min(edge_index.size(1), start + args.batch_size)
        edge_ids = torch.arange(start, end, device=device)
        logits = model.classify_edges(h, edge_index, edge_attr, edge_ids)
        probabilities.append(torch.sigmoid(logits).detach().cpu())
    probabilities = torch.cat(probabilities).tolist()

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    fields = [
        "graph_edge_id", "batch_code", "dataset_name", "source_row_no", "from_node_id", "to_node_id",
        "edge_type", "amount", "currency", "event_time", "label_fraud", "model_version", "feature_version",
        "risk_probability", "risk_score", "decision", "review_threshold", "block_threshold", "reason_json"
    ]
    decision_counts: Dict[str, int] = {}
    with open(args.output, "w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for row, probability in zip(identities, probabilities):
            decision = decision_for(probability, review_threshold, args.block_threshold)
            decision_counts[decision] = decision_counts.get(decision, 0) + 1
            reason = {
                "model": args.model_version,
                "feature_version": args.feature_version,
                "feature_mode": feature_mode,
                "feedback_mode": feedback_mode,
                "architecture": architecture,
                "edge_type": row["edge_type"],
                "amount": row["amount"],
                "threshold_source": "validation_best_f1" if args.review_threshold is None else "cli",
            }
            writer.writerow({
                **row,
                "model_version": args.model_version,
                "feature_version": args.feature_version,
                "risk_probability": f"{probability:.8f}",
                "risk_score": str(round(probability * 1000)),
                "decision": decision,
                "review_threshold": f"{review_threshold:.8f}",
                "block_threshold": f"{args.block_threshold:.8f}",
                "reason_json": json.dumps(reason, ensure_ascii=False, separators=(",", ":")),
            })

    summary = {
        "output": os.path.abspath(args.output),
        "num_edges": len(probabilities),
        "review_threshold": review_threshold,
        "block_threshold": args.block_threshold,
        "decision_counts": decision_counts,
        "data_meta": data_meta,
        "architecture": architecture,
        "checkpoint_epoch": checkpoint.get("epoch"),
        "checkpoint_val_metrics": val_metrics,
    }
    with open(args.output + ".summary.json", "w", encoding="utf-8") as handle:
        json.dump(summary, handle, indent=2, ensure_ascii=False)
    return summary


def main() -> None:
    args = parse_args()
    summary = score(args)
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
