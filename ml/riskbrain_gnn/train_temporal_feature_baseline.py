import argparse
import csv
import json
import math
import os
import pickle
import random
from collections import Counter, defaultdict, deque
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Optional, Tuple

import numpy as np


WINDOWS = (
    ("1h", 3600.0),
    ("24h", 86400.0),
    ("7d", 604800.0),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a leakage-safe temporal graph-feature baseline for iBank RiskBrain."
    )
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--split-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--model-version", default="riskbrain-temporal-feature-baseline-v5")
    parser.add_argument("--feature-version", default="riskgraph-temporal-v5")
    parser.add_argument("--algorithm", choices=["hist_gradient_boosting", "extra_trees"], default="hist_gradient_boosting")
    parser.add_argument("--max-edges", type=int, default=0)
    parser.add_argument("--edge-type-top-k", type=int, default=16)
    parser.add_argument("--currency-top-k", type=int, default=8)
    parser.add_argument("--positive-weight-cap", type=float, default=50.0)
    parser.add_argument("--random-state", type=int, default=20260530)
    parser.add_argument("--permutation-sample-size", type=int, default=5000)
    parser.add_argument("--review-threshold", type=float, default=None)
    parser.add_argument("--block-threshold", type=float, default=0.90)
    return parser.parse_args()


def read_csv(path: str) -> Iterable[Dict[str, str]]:
    with open(path, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            yield row


def as_int(value: Optional[str], default: int = 0) -> int:
    if value is None or value == "":
        return default
    try:
        return int(float(value))
    except ValueError:
        return default


def as_float(value: Optional[str], default: float = 0.0) -> float:
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


def log1p_amount(value: float) -> float:
    return math.log1p(max(0.0, value))


def safe_ratio(numerator: float, denominator: float) -> float:
    return numerator / max(1.0, denominator)


def amount_zscore(amount: float, count: float, total: float, total_sq: float) -> float:
    if count <= 1:
        return 0.0
    mean = total / count
    variance = max(0.0, total_sq / count - mean * mean)
    std = math.sqrt(variance)
    if std <= 1e-6:
        return 0.0
    return (amount - mean) / std


def elapsed_minutes_log(current_time: float, previous_time: float) -> float:
    if previous_time <= 0:
        return 0.0
    return math.log1p(max(0.0, current_time - previous_time) / 60.0)


def one_hot(value: str, categories: List[str]) -> List[float]:
    return [1.0 if value == category else 0.0 for category in categories] + [
        0.0 if value in categories else 1.0
    ]


def load_node_ids(data_dir: str) -> set:
    return {as_int(row.get("node_id"), -1) for row in read_csv(os.path.join(data_dir, "nodes.csv"))}


def load_eligible_edges(data_dir: str, max_edges: int = 0) -> List[Dict]:
    node_ids = load_node_ids(data_dir)
    rows = []
    for row_index, row in enumerate(read_csv(os.path.join(data_dir, "edges.csv"))):
        if max_edges > 0 and row_index >= max_edges:
            break
        if as_int(row.get("from_node_id"), -1) not in node_ids:
            continue
        if as_int(row.get("to_node_id"), -1) not in node_ids:
            continue
        model_index = len(rows)
        rows.append({
            "model_index": model_index,
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


def train_categories(edges: List[Dict], split_map: Dict[int, Dict], column: str, max_items: int) -> List[str]:
    counter = Counter()
    for item in edges:
        if split_map[item["model_index"]]["split"] == "train":
            counter[item["row"].get(column, "UNKNOWN") or "UNKNOWN"] += 1
    return [value for value, _ in counter.most_common(max_items)]


def empty_node_state() -> Dict:
    return {
        "in_count": 0.0,
        "out_count": 0.0,
        "in_amount": 0.0,
        "out_amount": 0.0,
        "in_amount_sq": 0.0,
        "out_amount_sq": 0.0,
        "last_in_time": 0.0,
        "last_out_time": 0.0,
        "in_sources": set(),
        "out_targets": set(),
    }


def empty_pair_state() -> Dict:
    return {
        "count": 0.0,
        "amount": 0.0,
        "amount_sq": 0.0,
        "last_time": 0.0,
    }


def window_stats(store: Dict[Tuple[int, str], deque], node_or_pair, suffix: str,
                 current_time: float, window_seconds: float) -> Tuple[float, float]:
    key = (node_or_pair, suffix)
    queue = store[key]
    cutoff = current_time - window_seconds
    while queue and queue[0][0] < cutoff:
        queue.popleft()
    return float(len(queue)), float(sum(amount for _, amount in queue))


def update_window(store: Dict[Tuple[int, str], deque], node_or_pair, suffix: str,
                  current_time: float, amount: float) -> None:
    store[(node_or_pair, suffix)].append((current_time, amount))


def build_feature_names(edge_types: List[str], currencies: List[str]) -> List[str]:
    names = [
        "log_amount",
        "amount_is_round_100",
        "amount_is_round_1000",
        "hour_norm",
        "day_norm",
        "is_weekend",
        "is_night",
        "self_loop",
        "src_in_count_log",
        "src_out_count_log",
        "dst_in_count_log",
        "dst_out_count_log",
        "src_in_amount_log",
        "src_out_amount_log",
        "dst_in_amount_log",
        "dst_out_amount_log",
        "src_out_avg_amount_log",
        "dst_in_avg_amount_log",
        "pair_avg_amount_log",
        "amount_to_src_out_avg",
        "amount_to_dst_in_avg",
        "amount_to_pair_avg",
        "src_out_amount_zscore",
        "dst_in_amount_zscore",
        "pair_amount_zscore",
        "pair_count_log",
        "pair_amount_log",
        "is_new_pair",
        "src_unique_out_targets_log",
        "src_unique_in_sources_log",
        "dst_unique_in_sources_log",
        "dst_unique_out_targets_log",
        "src_out_target_diversity",
        "dst_in_source_diversity",
        "minutes_since_src_out_log",
        "minutes_since_src_any_log",
        "minutes_since_dst_in_log",
        "minutes_since_dst_any_log",
        "minutes_since_pair_log",
    ]
    for label, _ in WINDOWS:
        names.extend([
            f"src_out_count_{label}_log",
            f"src_out_amount_{label}_log",
            f"dst_in_count_{label}_log",
            f"dst_in_amount_{label}_log",
            f"pair_count_{label}_log",
            f"pair_amount_{label}_log",
            f"amount_to_src_out_amount_{label}",
            f"amount_to_dst_in_amount_{label}",
            f"amount_to_pair_amount_{label}",
        ])
    names += [f"edge_type_{value}" for value in edge_types]
    names.append("edge_type_UNKNOWN")
    names += [f"currency_{value}" for value in currencies]
    names.append("currency_UNKNOWN")
    return names


def build_temporal_features(edges: List[Dict], split_map: Dict[int, Dict],
                            edge_types: List[str], currencies: List[str]) -> Tuple[np.ndarray, np.ndarray, List[Dict], List[str]]:
    ordered = sorted(edges, key=lambda item: (split_map[item["model_index"]]["order_rank"], item["row_index"]))
    feature_names = build_feature_names(edge_types, currencies)
    x_by_index: Dict[int, List[float]] = {}
    y_by_index: Dict[int, int] = {}
    node_stats = defaultdict(empty_node_state)
    pair_stats = defaultdict(empty_pair_state)
    windows = defaultdict(deque)

    for item in ordered:
        row = item["row"]
        model_index = item["model_index"]
        src_id = as_int(row.get("from_node_id"), -1)
        dst_id = as_int(row.get("to_node_id"), -1)
        pair_key = (src_id, dst_id)
        amount = max(0.0, as_float(row.get("amount"), 0.0))
        current_time = split_map[model_index].get("event_order") or item["event_order"]
        src = node_stats[src_id]
        dst = node_stats[dst_id]
        pair = pair_stats[pair_key]
        src_any_time = max(src["last_in_time"], src["last_out_time"])
        dst_any_time = max(dst["last_in_time"], dst["last_out_time"])

        parsed_time = datetime.fromtimestamp(current_time)
        hour = parsed_time.hour
        day = parsed_time.weekday()
        src_out_avg = src["out_amount"] / src["out_count"] if src["out_count"] else 0.0
        dst_in_avg = dst["in_amount"] / dst["in_count"] if dst["in_count"] else 0.0
        pair_avg = pair["amount"] / pair["count"] if pair["count"] else 0.0

        features = [
            log1p_amount(amount),
            1.0 if amount > 0 and abs(amount % 100.0) < 1e-6 else 0.0,
            1.0 if amount > 0 and abs(amount % 1000.0) < 1e-6 else 0.0,
            hour / 23.0,
            day / 6.0,
            1.0 if day >= 5 else 0.0,
            1.0 if hour < 6 or hour >= 22 else 0.0,
            1.0 if src_id == dst_id else 0.0,
            math.log1p(src["in_count"]),
            math.log1p(src["out_count"]),
            math.log1p(dst["in_count"]),
            math.log1p(dst["out_count"]),
            log1p_amount(src["in_amount"]),
            log1p_amount(src["out_amount"]),
            log1p_amount(dst["in_amount"]),
            log1p_amount(dst["out_amount"]),
            log1p_amount(src_out_avg),
            log1p_amount(dst_in_avg),
            log1p_amount(pair_avg),
            safe_ratio(amount, src_out_avg),
            safe_ratio(amount, dst_in_avg),
            safe_ratio(amount, pair_avg),
            amount_zscore(amount, src["out_count"], src["out_amount"], src["out_amount_sq"]),
            amount_zscore(amount, dst["in_count"], dst["in_amount"], dst["in_amount_sq"]),
            amount_zscore(amount, pair["count"], pair["amount"], pair["amount_sq"]),
            math.log1p(pair["count"]),
            log1p_amount(pair["amount"]),
            1.0 if pair["count"] == 0 else 0.0,
            math.log1p(len(src["out_targets"])),
            math.log1p(len(src["in_sources"])),
            math.log1p(len(dst["in_sources"])),
            math.log1p(len(dst["out_targets"])),
            safe_ratio(len(src["out_targets"]), src["out_count"]),
            safe_ratio(len(dst["in_sources"]), dst["in_count"]),
            elapsed_minutes_log(current_time, src["last_out_time"]),
            elapsed_minutes_log(current_time, src_any_time),
            elapsed_minutes_log(current_time, dst["last_in_time"]),
            elapsed_minutes_log(current_time, dst_any_time),
            elapsed_minutes_log(current_time, pair["last_time"]),
        ]

        for label, seconds in WINDOWS:
            src_count, src_amount = window_stats(windows, src_id, f"out_{label}", current_time, seconds)
            dst_count, dst_amount = window_stats(windows, dst_id, f"in_{label}", current_time, seconds)
            pair_count, pair_amount = window_stats(windows, pair_key, f"pair_{label}", current_time, seconds)
            features.extend([
                math.log1p(src_count),
                log1p_amount(src_amount),
                math.log1p(dst_count),
                log1p_amount(dst_amount),
                math.log1p(pair_count),
                log1p_amount(pair_amount),
                safe_ratio(amount, src_amount),
                safe_ratio(amount, dst_amount),
                safe_ratio(amount, pair_amount),
            ])

        features.extend(one_hot(row.get("edge_type", "UNKNOWN") or "UNKNOWN", edge_types))
        features.extend(one_hot(row.get("currency", "UNKNOWN") or "UNKNOWN", currencies))
        if len(features) != len(feature_names):
            raise RuntimeError(f"feature length mismatch: {len(features)} vs {len(feature_names)}")
        x_by_index[model_index] = features
        y_by_index[model_index] = 1 if as_int(row.get("label_fraud"), 0) == 1 else 0

        src["out_count"] += 1.0
        src["out_amount"] += amount
        src["out_amount_sq"] += amount * amount
        src["last_out_time"] = current_time
        src["out_targets"].add(dst_id)
        dst["in_count"] += 1.0
        dst["in_amount"] += amount
        dst["in_amount_sq"] += amount * amount
        dst["last_in_time"] = current_time
        dst["in_sources"].add(src_id)
        pair["count"] += 1.0
        pair["amount"] += amount
        pair["amount_sq"] += amount * amount
        pair["last_time"] = current_time
        for label, _ in WINDOWS:
            update_window(windows, src_id, f"out_{label}", current_time, amount)
            update_window(windows, dst_id, f"in_{label}", current_time, amount)
            update_window(windows, pair_key, f"pair_{label}", current_time, amount)

    rows = sorted(edges, key=lambda item: item["model_index"])
    x = np.asarray([x_by_index[item["model_index"]] for item in rows], dtype=np.float32)
    y = np.asarray([y_by_index[item["model_index"]] for item in rows], dtype=np.int32)
    return x, y, rows, feature_names


def split_indices(split_map: Dict[int, Dict], split: str) -> np.ndarray:
    values = [model_index for model_index, meta in split_map.items() if meta["split"] == split]
    return np.asarray(sorted(values), dtype=np.int64)


def compute_sample_weight(y: np.ndarray, train_ids: np.ndarray, cap: float) -> np.ndarray:
    train_y = y[train_ids]
    positives = float(np.sum(train_y == 1))
    negatives = float(np.sum(train_y == 0))
    positive_weight = min(cap, negatives / max(1.0, positives))
    weights = np.ones_like(y, dtype=np.float32)
    weights[y == 1] = positive_weight
    return weights


def make_model(args: argparse.Namespace):
    if args.algorithm == "extra_trees":
        from sklearn.ensemble import ExtraTreesClassifier
        return ExtraTreesClassifier(
            n_estimators=320,
            max_depth=18,
            min_samples_leaf=4,
            class_weight=None,
            n_jobs=-1,
            random_state=args.random_state,
        )
    from sklearn.ensemble import HistGradientBoostingClassifier
    return HistGradientBoostingClassifier(
        max_iter=320,
        learning_rate=0.05,
        max_leaf_nodes=31,
        min_samples_leaf=30,
        l2_regularization=0.01,
        early_stopping=True,
        validation_fraction=0.12,
        random_state=args.random_state,
    )


def probability_for(model, x: np.ndarray) -> np.ndarray:
    if hasattr(model, "predict_proba"):
        return model.predict_proba(x)[:, 1]
    scores = model.decision_function(x)
    return 1.0 / (1.0 + np.exp(-scores))


def auc_ap(probabilities: np.ndarray, labels: np.ndarray) -> Tuple[float, float]:
    from sklearn.metrics import average_precision_score, roc_auc_score
    if len(np.unique(labels)) < 2:
        return float("nan"), float("nan")
    return float(roc_auc_score(labels, probabilities)), float(average_precision_score(labels, probabilities))


def fixed_threshold_metrics(probabilities: np.ndarray, labels: np.ndarray, threshold: float) -> Dict[str, float]:
    preds = (probabilities >= threshold).astype(np.int32)
    tp = int(np.sum((preds == 1) & (labels == 1)))
    fp = int(np.sum((preds == 1) & (labels == 0)))
    fn = int(np.sum((preds == 0) & (labels == 1)))
    tn = int(np.sum((preds == 0) & (labels == 0)))
    precision = tp / max(1, tp + fp)
    recall = tp / max(1, tp + fn)
    f1 = 2 * precision * recall / max(1e-12, precision + recall)
    auc, ap = auc_ap(probabilities, labels)
    return {
        "threshold": float(threshold),
        "auc": auc,
        "ap": ap,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "tn": tn,
        "positive_edges": int(np.sum(labels == 1)),
        "total_edges": int(labels.shape[0]),
    }


def best_f1_metrics(probabilities: np.ndarray, labels: np.ndarray) -> Dict[str, float]:
    if probabilities.shape[0] == 0:
        return fixed_threshold_metrics(probabilities, labels, 0.5)
    order = np.argsort(-probabilities)
    sorted_probs = probabilities[order]
    sorted_labels = labels[order]
    tp_curve = np.cumsum(sorted_labels == 1)
    rank = np.arange(1, sorted_labels.shape[0] + 1)
    positive_count = max(1, int(np.sum(labels == 1)))
    precision = tp_curve / rank
    recall = tp_curve / positive_count
    f1 = 2 * precision * recall / np.maximum(1e-12, precision + recall)
    best_idx = int(np.argmax(f1))
    threshold = float(sorted_probs[best_idx])
    result = fixed_threshold_metrics(probabilities, labels, threshold)
    result.update({
        "best_threshold": threshold,
        "best_precision": float(precision[best_idx]),
        "best_recall": float(recall[best_idx]),
        "best_f1": float(f1[best_idx]),
    })
    return result


def write_feature_importance(model, x_val: np.ndarray, y_val: np.ndarray, feature_names: List[str],
                             output_dir: str, sample_size: int, random_state: int) -> List[Dict]:
    importance = None
    source = None
    if hasattr(model, "feature_importances_"):
        importance = np.asarray(model.feature_importances_, dtype=np.float64)
        source = "model_feature_importances"
    elif sample_size > 0 and x_val.shape[0] > 1 and len(np.unique(y_val)) > 1:
        from sklearn.inspection import permutation_importance
        rng = np.random.default_rng(random_state)
        ids = np.arange(x_val.shape[0])
        if x_val.shape[0] > sample_size:
            ids = rng.choice(ids, size=sample_size, replace=False)
        result = permutation_importance(
            model,
            x_val[ids],
            y_val[ids],
            scoring="average_precision",
            n_repeats=3,
            random_state=random_state,
            n_jobs=1,
        )
        importance = result.importances_mean
        source = "validation_permutation_average_precision"
    else:
        importance = np.zeros(len(feature_names), dtype=np.float64)
        source = "unavailable"

    rows = []
    for name, value in sorted(zip(feature_names, importance), key=lambda item: abs(item[1]), reverse=True):
        rows.append({"feature": name, "importance": float(value), "source": source})
    path = os.path.join(output_dir, "feature_importance.csv")
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["feature", "importance", "source"])
        writer.writeheader()
        writer.writerows(rows)
    return rows


def decision_for(probability: float, review_threshold: float, block_threshold: float) -> str:
    if probability >= block_threshold:
        return "BLOCK"
    if probability >= review_threshold:
        return "REVIEW"
    return "PASS"


def write_edge_scores(path: str, rows: List[Dict], probabilities: np.ndarray, feature_matrix: np.ndarray,
                      feature_names: List[str], top_features: List[Dict], args: argparse.Namespace,
                      review_threshold: float) -> Dict[str, int]:
    fields = [
        "graph_edge_id", "batch_code", "dataset_name", "source_row_no", "from_node_id", "to_node_id",
        "edge_type", "amount", "currency", "event_time", "label_fraud", "model_version", "feature_version",
        "risk_probability", "risk_score", "decision", "review_threshold", "block_threshold", "reason_json"
    ]
    important_names = [item["feature"] for item in top_features[:8]]
    important_indexes = [feature_names.index(name) for name in important_names if name in feature_names]
    decision_counts: Dict[str, int] = {}
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for item, probability, features in zip(rows, probabilities, feature_matrix):
            row = item["row"]
            decision = decision_for(float(probability), review_threshold, args.block_threshold)
            decision_counts[decision] = decision_counts.get(decision, 0) + 1
            reason_features = [
                {"name": feature_names[index], "value": round(float(features[index]), 6)}
                for index in important_indexes[:5]
            ]
            reason = {
                "model": args.model_version,
                "feature_version": args.feature_version,
                "algorithm": args.algorithm,
                "feature_mode": "temporal_v1",
                "feedback_mode": "off",
                "split_mode": "time",
                "threshold_source": "validation_best_f1" if args.review_threshold is None else "cli",
                "top_global_features_for_edge": reason_features,
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
    try:
        import sklearn  # noqa: F401
    except ImportError as exc:
        raise RuntimeError("scikit-learn is required for train_temporal_feature_baseline.py") from exc

    random.seed(args.random_state)
    np.random.seed(args.random_state)
    os.makedirs(args.output_dir, exist_ok=True)
    edges = load_eligible_edges(args.data_dir, args.max_edges)
    split_map = load_split(args.split_file)
    if set(split_map.keys()) != {item["model_index"] for item in edges}:
        raise ValueError("split file assignments do not match eligible edges from data-dir/max-edges")

    edge_types = train_categories(edges, split_map, "edge_type", args.edge_type_top_k)
    currencies = train_categories(edges, split_map, "currency", args.currency_top_k)
    x, y, rows, feature_names = build_temporal_features(edges, split_map, edge_types, currencies)
    train_ids = split_indices(split_map, "train")
    val_ids = split_indices(split_map, "val")
    test_ids = split_indices(split_map, "test")
    sample_weight = compute_sample_weight(y, train_ids, args.positive_weight_cap)

    model = make_model(args)
    model.fit(x[train_ids], y[train_ids], sample_weight=sample_weight[train_ids])
    val_probs = probability_for(model, x[val_ids])
    test_probs = probability_for(model, x[test_ids])
    all_probs = probability_for(model, x)
    val_metrics = best_f1_metrics(val_probs, y[val_ids])
    review_threshold = args.review_threshold if args.review_threshold is not None else float(val_metrics["best_threshold"])
    test_metrics = fixed_threshold_metrics(test_probs, y[test_ids], review_threshold)
    test_oracle_metrics = best_f1_metrics(test_probs, y[test_ids])
    importance_rows = write_feature_importance(
        model, x[val_ids], y[val_ids], feature_names, args.output_dir,
        args.permutation_sample_size, args.random_state)

    edge_score_path = os.path.join(args.output_dir, "edge_scores.csv")
    decision_counts = write_edge_scores(
        edge_score_path, rows, all_probs, x, feature_names, importance_rows,
        args, review_threshold)
    summary = {
        "output": os.path.abspath(edge_score_path),
        "num_edges": int(x.shape[0]),
        "review_threshold": review_threshold,
        "block_threshold": args.block_threshold,
        "decision_counts": decision_counts,
    }
    with open(edge_score_path + ".summary.json", "w", encoding="utf-8") as handle:
        json.dump(summary, handle, indent=2, ensure_ascii=False)

    meta = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "data_dir": os.path.abspath(args.data_dir),
        "split_file": os.path.abspath(args.split_file),
        "model_version": args.model_version,
        "feature_version": args.feature_version,
        "algorithm": args.algorithm,
        "feature_mode": "temporal_v1",
        "feedback_mode": "off",
        "split_mode": "time",
        "effective_split_mode": "external_temporal",
        "standardize_scope": "train",
        "adjacency_scope": "train",
        "normalization": {
            "scope": "not_required_tree_model",
            "reason": "tree baseline consumes raw engineered temporal features; no scaler is fit on validation/test",
        },
        "train_edges": int(train_ids.shape[0]),
        "val_edges": int(val_ids.shape[0]),
        "test_edges": int(test_ids.shape[0]),
        "train_positive_edges": int(np.sum(y[train_ids] == 1)),
        "val_positive_edges": int(np.sum(y[val_ids] == 1)),
        "test_positive_edges": int(np.sum(y[test_ids] == 1)),
        "positive_weight_cap": args.positive_weight_cap,
        "edge_type_categories": edge_types,
        "currency_categories": currencies,
        "feature_count": len(feature_names),
        "features": feature_names,
    }
    result = {
        "meta": meta,
        "best_val_metrics": val_metrics,
        "test_metrics": test_metrics,
        "test_oracle_metrics": test_oracle_metrics,
        "top_features": importance_rows[:20],
        "score_summary": summary,
    }
    with open(os.path.join(args.output_dir, "metrics.json"), "w", encoding="utf-8") as handle:
        json.dump(result, handle, indent=2, ensure_ascii=False)
    with open(os.path.join(args.output_dir, "model.pkl"), "wb") as handle:
        pickle.dump({
            "model": model,
            "meta": meta,
            "feature_names": feature_names,
            "edge_type_categories": edge_types,
            "currency_categories": currencies,
        }, handle)
    return result


def main() -> None:
    args = parse_args()
    result = train(args)
    print(json.dumps({
        "best_val_metrics": result["best_val_metrics"],
        "test_metrics": result["test_metrics"],
        "test_oracle_metrics": result["test_oracle_metrics"],
        "score_summary": result["score_summary"],
        "top_features": result["top_features"][:10],
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
