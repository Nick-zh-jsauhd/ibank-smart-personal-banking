import argparse
import csv
import json
import math
import os
import pickle
import random
from collections import defaultdict, deque
from datetime import datetime, timezone
from typing import Dict, List, Tuple

import numpy as np

from train_temporal_feature_baseline import (
    WINDOWS,
    amount_zscore,
    as_float,
    as_int,
    best_f1_metrics,
    build_temporal_features,
    compute_sample_weight,
    decision_for,
    elapsed_minutes_log,
    fixed_threshold_metrics,
    load_eligible_edges,
    load_split,
    log1p_amount,
    make_model,
    probability_for,
    safe_ratio,
    train_categories,
    write_feature_importance,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a leakage-safe line-graph feature baseline for iBank RiskBrain."
    )
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--split-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--model-version", default="riskbrain-linegraph-feature-baseline-v7")
    parser.add_argument("--feature-version", default="riskgraph-linegraph-v7")
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


def empty_node_line_state() -> Dict:
    return {
        "in_count": 0.0,
        "out_count": 0.0,
        "in_amount": 0.0,
        "out_amount": 0.0,
        "in_amount_sq": 0.0,
        "out_amount_sq": 0.0,
        "last_in_time": 0.0,
        "last_out_time": 0.0,
        "last_in_amount": 0.0,
        "last_out_amount": 0.0,
        "last_in_counterparty": -1,
        "last_out_counterparty": -1,
        "in_counterparties": set(),
        "out_counterparties": set(),
    }


def empty_pair_line_state() -> Dict:
    return {
        "count": 0.0,
        "amount": 0.0,
        "amount_sq": 0.0,
        "last_time": 0.0,
        "last_amount": 0.0,
    }


def window_line_stats(store: Dict[Tuple[object, str], deque], key, suffix: str,
                      current_time: float, window_seconds: float) -> Tuple[float, float, float]:
    queue = store[(key, suffix)]
    cutoff = current_time - window_seconds
    while queue and queue[0][0] < cutoff:
        queue.popleft()
    counterparties = {counterparty for _, _, counterparty in queue}
    return float(len(queue)), float(sum(amount for _, amount, _ in queue)), float(len(counterparties))


def update_line_window(store: Dict[Tuple[object, str], deque], key, suffix: str,
                       current_time: float, amount: float, counterparty: int) -> None:
    store[(key, suffix)].append((current_time, amount, counterparty))


def line_feature_names() -> List[str]:
    names = [
        "line_src_prior_in_count_log",
        "line_src_prior_out_count_log",
        "line_dst_prior_in_count_log",
        "line_dst_prior_out_count_log",
        "line_src_prior_in_amount_log",
        "line_src_prior_out_amount_log",
        "line_dst_prior_in_amount_log",
        "line_dst_prior_out_amount_log",
        "line_src_in_to_out_amount_ratio",
        "line_dst_out_to_in_amount_ratio",
        "line_amount_to_src_last_in",
        "line_amount_to_dst_last_out",
        "line_src_in_amount_zscore",
        "line_src_out_amount_zscore",
        "line_dst_in_amount_zscore",
        "line_dst_out_amount_zscore",
        "line_minutes_since_src_in_log",
        "line_minutes_since_src_out_log",
        "line_minutes_since_dst_in_log",
        "line_minutes_since_dst_out_log",
        "line_reverse_pair_count_log",
        "line_reverse_pair_amount_log",
        "line_minutes_since_reverse_pair_log",
        "line_reverse_amount_ratio",
        "line_is_immediate_pass_through",
        "line_is_possible_round_trip",
        "line_src_counterparty_reuse",
        "line_dst_counterparty_reuse",
        "line_src_unique_in_log",
        "line_src_unique_out_log",
        "line_dst_unique_in_log",
        "line_dst_unique_out_log",
        "line_src_in_out_diversity_ratio",
        "line_dst_out_in_diversity_ratio",
    ]
    for label, _ in WINDOWS:
        names.extend([
            f"line_src_in_count_{label}_log",
            f"line_src_in_amount_{label}_log",
            f"line_src_in_unique_{label}_log",
            f"line_src_out_count_{label}_log",
            f"line_src_out_amount_{label}_log",
            f"line_src_out_unique_{label}_log",
            f"line_dst_in_count_{label}_log",
            f"line_dst_in_amount_{label}_log",
            f"line_dst_in_unique_{label}_log",
            f"line_dst_out_count_{label}_log",
            f"line_dst_out_amount_{label}_log",
            f"line_dst_out_unique_{label}_log",
            f"line_reverse_pair_count_{label}_log",
            f"line_reverse_pair_amount_{label}_log",
            f"line_reverse_pair_unique_{label}_log",
            f"line_amount_to_src_in_amount_{label}",
            f"line_amount_to_dst_out_amount_{label}",
            f"line_amount_to_reverse_pair_amount_{label}",
            f"line_rapid_pass_through_{label}",
            f"line_fan_in_to_out_{label}",
            f"line_fan_out_from_dst_{label}",
        ])
    return names


def build_line_graph_features(edges: List[Dict], split_map: Dict[int, Dict]) -> Tuple[np.ndarray, List[str]]:
    ordered = sorted(edges, key=lambda item: (split_map[item["model_index"]]["order_rank"], item["row_index"]))
    names = line_feature_names()
    features_by_index: Dict[int, List[float]] = {}
    node_state = defaultdict(empty_node_line_state)
    pair_state = defaultdict(empty_pair_line_state)
    windows = defaultdict(deque)

    for item in ordered:
        row = item["row"]
        model_index = item["model_index"]
        src_id = as_int(row.get("from_node_id"), -1)
        dst_id = as_int(row.get("to_node_id"), -1)
        pair_key = (src_id, dst_id)
        reverse_key = (dst_id, src_id)
        amount = max(0.0, as_float(row.get("amount"), 0.0))
        current_time = split_map[model_index].get("event_order") or item["event_order"]

        src = node_state[src_id]
        dst = node_state[dst_id]
        reverse = pair_state[reverse_key]
        src_in_out_ratio = safe_ratio(src["in_amount"], src["out_amount"])
        dst_out_in_ratio = safe_ratio(dst["out_amount"], dst["in_amount"])
        reverse_amount_ratio = safe_ratio(amount, reverse["last_amount"])
        minutes_since_src_in = elapsed_minutes_log(current_time, src["last_in_time"])
        minutes_since_dst_out = elapsed_minutes_log(current_time, dst["last_out_time"])
        immediate_pass = 1.0 if src["last_in_time"] > 0 and current_time - src["last_in_time"] <= 3600.0 else 0.0
        round_trip = 1.0 if reverse["last_time"] > 0 and current_time - reverse["last_time"] <= 86400.0 else 0.0
        src_counterparty_reuse = 1.0 if dst_id in src["out_counterparties"] else 0.0
        dst_counterparty_reuse = 1.0 if src_id in dst["in_counterparties"] else 0.0

        values = [
            math.log1p(src["in_count"]),
            math.log1p(src["out_count"]),
            math.log1p(dst["in_count"]),
            math.log1p(dst["out_count"]),
            log1p_amount(src["in_amount"]),
            log1p_amount(src["out_amount"]),
            log1p_amount(dst["in_amount"]),
            log1p_amount(dst["out_amount"]),
            src_in_out_ratio,
            dst_out_in_ratio,
            safe_ratio(amount, src["last_in_amount"]),
            safe_ratio(amount, dst["last_out_amount"]),
            amount_zscore(amount, src["in_count"], src["in_amount"], src["in_amount_sq"]),
            amount_zscore(amount, src["out_count"], src["out_amount"], src["out_amount_sq"]),
            amount_zscore(amount, dst["in_count"], dst["in_amount"], dst["in_amount_sq"]),
            amount_zscore(amount, dst["out_count"], dst["out_amount"], dst["out_amount_sq"]),
            minutes_since_src_in,
            elapsed_minutes_log(current_time, src["last_out_time"]),
            elapsed_minutes_log(current_time, dst["last_in_time"]),
            minutes_since_dst_out,
            math.log1p(reverse["count"]),
            log1p_amount(reverse["amount"]),
            elapsed_minutes_log(current_time, reverse["last_time"]),
            reverse_amount_ratio,
            immediate_pass,
            round_trip,
            src_counterparty_reuse,
            dst_counterparty_reuse,
            math.log1p(len(src["in_counterparties"])),
            math.log1p(len(src["out_counterparties"])),
            math.log1p(len(dst["in_counterparties"])),
            math.log1p(len(dst["out_counterparties"])),
            safe_ratio(len(src["in_counterparties"]), len(src["out_counterparties"])),
            safe_ratio(len(dst["out_counterparties"]), len(dst["in_counterparties"])),
        ]

        for label, seconds in WINDOWS:
            src_in_count, src_in_amount, src_in_unique = window_line_stats(windows, src_id, f"in_{label}", current_time, seconds)
            src_out_count, src_out_amount, src_out_unique = window_line_stats(windows, src_id, f"out_{label}", current_time, seconds)
            dst_in_count, dst_in_amount, dst_in_unique = window_line_stats(windows, dst_id, f"in_{label}", current_time, seconds)
            dst_out_count, dst_out_amount, dst_out_unique = window_line_stats(windows, dst_id, f"out_{label}", current_time, seconds)
            reverse_count, reverse_amount, reverse_unique = window_line_stats(windows, reverse_key, f"pair_{label}", current_time, seconds)
            rapid_pass = 1.0 if src_in_count > 0 and safe_ratio(amount, src_in_amount) >= 0.7 else 0.0
            fan_in_to_out = safe_ratio(src_in_unique, src_out_unique)
            fan_out_from_dst = safe_ratio(dst_out_unique, dst_in_unique)
            values.extend([
                math.log1p(src_in_count),
                log1p_amount(src_in_amount),
                math.log1p(src_in_unique),
                math.log1p(src_out_count),
                log1p_amount(src_out_amount),
                math.log1p(src_out_unique),
                math.log1p(dst_in_count),
                log1p_amount(dst_in_amount),
                math.log1p(dst_in_unique),
                math.log1p(dst_out_count),
                log1p_amount(dst_out_amount),
                math.log1p(dst_out_unique),
                math.log1p(reverse_count),
                log1p_amount(reverse_amount),
                math.log1p(reverse_unique),
                safe_ratio(amount, src_in_amount),
                safe_ratio(amount, dst_out_amount),
                safe_ratio(amount, reverse_amount),
                rapid_pass,
                fan_in_to_out,
                fan_out_from_dst,
            ])

        if len(values) != len(names):
            raise RuntimeError(f"line feature length mismatch: {len(values)} vs {len(names)}")
        features_by_index[model_index] = values

        src["out_count"] += 1.0
        src["out_amount"] += amount
        src["out_amount_sq"] += amount * amount
        src["last_out_time"] = current_time
        src["last_out_amount"] = amount
        src["last_out_counterparty"] = dst_id
        src["out_counterparties"].add(dst_id)
        dst["in_count"] += 1.0
        dst["in_amount"] += amount
        dst["in_amount_sq"] += amount * amount
        dst["last_in_time"] = current_time
        dst["last_in_amount"] = amount
        dst["last_in_counterparty"] = src_id
        dst["in_counterparties"].add(src_id)
        pair = pair_state[pair_key]
        pair["count"] += 1.0
        pair["amount"] += amount
        pair["amount_sq"] += amount * amount
        pair["last_time"] = current_time
        pair["last_amount"] = amount
        for label, _ in WINDOWS:
            update_line_window(windows, src_id, f"out_{label}", current_time, amount, dst_id)
            update_line_window(windows, dst_id, f"in_{label}", current_time, amount, src_id)
            update_line_window(windows, pair_key, f"pair_{label}", current_time, amount, dst_id)

    rows = sorted(edges, key=lambda item: item["model_index"])
    matrix = np.asarray([features_by_index[item["model_index"]] for item in rows], dtype=np.float32)
    return matrix, names


def split_indices(split_map: Dict[int, Dict], split: str) -> np.ndarray:
    values = [model_index for model_index, meta in split_map.items() if meta["split"] == split]
    return np.asarray(sorted(values), dtype=np.int64)


def write_line_edge_scores(path: str, rows: List[Dict], probabilities: np.ndarray, feature_matrix: np.ndarray,
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
                "feature_mode": "line_temporal_v1",
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
        raise RuntimeError("scikit-learn is required for train_line_graph_feature_baseline.py") from exc

    random.seed(args.random_state)
    np.random.seed(args.random_state)
    os.makedirs(args.output_dir, exist_ok=True)
    edges = load_eligible_edges(args.data_dir, args.max_edges)
    split_map = load_split(args.split_file)
    if set(split_map.keys()) != {item["model_index"] for item in edges}:
        raise ValueError("split file assignments do not match eligible edges from data-dir/max-edges")

    edge_types = train_categories(edges, split_map, "edge_type", args.edge_type_top_k)
    currencies = train_categories(edges, split_map, "currency", args.currency_top_k)
    temporal_x, y, rows, temporal_names = build_temporal_features(edges, split_map, edge_types, currencies)
    line_x, line_names = build_line_graph_features(edges, split_map)
    x = np.concatenate([temporal_x, line_x], axis=1).astype(np.float32)
    feature_names = temporal_names + line_names
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
    decision_counts = write_line_edge_scores(
        edge_score_path, rows, all_probs, x, feature_names, importance_rows, args, review_threshold)
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
        "feature_mode": "line_temporal_v1",
        "feedback_mode": "off",
        "split_mode": "time",
        "effective_split_mode": "external_temporal",
        "standardize_scope": "train",
        "adjacency_scope": "train",
        "normalization": {
            "scope": "not_required_tree_model",
            "reason": "tree baseline consumes raw engineered line-graph temporal features; no scaler is fit",
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
        "temporal_feature_count": len(temporal_names),
        "line_graph_feature_count": len(line_names),
        "feature_count": len(feature_names),
        "features": feature_names,
        "leakage_policy": {
            "label_columns_used_as_features": False,
            "feedback_columns_used_as_features": False,
            "category_vocab_scope": "train_split_only",
            "line_graph_history_scope": "prior_transactions_only",
            "validation_threshold_only": True,
        },
    }
    result = {
        "meta": meta,
        "best_val_metrics": val_metrics,
        "test_metrics": test_metrics,
        "test_oracle_metrics": test_oracle_metrics,
        "top_features": importance_rows[:30],
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
        "top_features": result["top_features"][:12],
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
