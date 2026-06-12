import argparse
import csv
import json
import os
from datetime import datetime, timezone

from train_temporal_feature_baseline import (
    build_temporal_features,
    load_eligible_edges,
    load_split,
    train_categories,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export prefix-only temporal graph features for GNN and baseline model reuse."
    )
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--split-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--max-edges", type=int, default=0)
    parser.add_argument("--edge-type-top-k", type=int, default=16)
    parser.add_argument("--currency-top-k", type=int, default=8)
    return parser.parse_args()


def write_features(args: argparse.Namespace) -> dict:
    os.makedirs(args.output_dir, exist_ok=True)
    edges = load_eligible_edges(args.data_dir, args.max_edges)
    split_map = load_split(args.split_file)
    if set(split_map.keys()) != {item["model_index"] for item in edges}:
        raise ValueError("split file assignments do not match eligible edges from data-dir/max-edges")

    edge_types = train_categories(edges, split_map, "edge_type", args.edge_type_top_k)
    currencies = train_categories(edges, split_map, "currency", args.currency_top_k)
    x, y, rows, feature_names = build_temporal_features(edges, split_map, edge_types, currencies)

    feature_columns = [f"f{i:03d}" for i in range(len(feature_names))]
    feature_path = os.path.join(args.output_dir, "temporal_edge_features.csv")
    fields = ["model_index", "edge_id", "split", "label_fraud"] + feature_columns
    split_counts = {"train": 0, "val": 0, "test": 0}
    positive_counts = {"train": 0, "val": 0, "test": 0}

    with open(feature_path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for item, label, values in zip(rows, y.tolist(), x.tolist()):
            row = item["row"]
            split = split_map[item["model_index"]]["split"]
            split_counts[split] = split_counts.get(split, 0) + 1
            positive_counts[split] = positive_counts.get(split, 0) + int(label)
            out = {
                "model_index": item["model_index"],
                "edge_id": row.get("edge_id", ""),
                "split": split,
                "label_fraud": int(label),
            }
            out.update({column: f"{float(value):.8g}" for column, value in zip(feature_columns, values)})
            writer.writerow(out)

    meta = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "protocol_version": "temporal-edge-features-v1",
        "data_dir": os.path.abspath(args.data_dir),
        "split_file": os.path.abspath(args.split_file),
        "feature_file": os.path.abspath(feature_path),
        "num_edges": int(x.shape[0]),
        "feature_count": len(feature_names),
        "feature_columns": feature_columns,
        "feature_names": feature_names,
        "edge_type_categories": edge_types,
        "currency_categories": currencies,
        "split_counts": split_counts,
        "positive_counts": positive_counts,
        "leakage_policy": {
            "label_columns_used_as_features": False,
            "feedback_columns_used_as_features": False,
            "category_vocab_scope": "train_split_only",
            "history_scope": "events_before_current_edge_only",
        },
    }
    meta_path = os.path.join(args.output_dir, "temporal_edge_feature_meta.json")
    with open(meta_path, "w", encoding="utf-8") as handle:
        json.dump(meta, handle, indent=2, ensure_ascii=False)
    return meta


def main() -> None:
    args = parse_args()
    meta = write_features(args)
    print(json.dumps({
        "feature_file": meta["feature_file"],
        "num_edges": meta["num_edges"],
        "feature_count": meta["feature_count"],
        "split_counts": meta["split_counts"],
        "positive_counts": meta["positive_counts"],
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
