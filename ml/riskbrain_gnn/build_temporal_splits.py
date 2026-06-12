import argparse
import csv
import json
import math
import os
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Optional, Tuple


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build auditable chronological train/validation/test splits for iBank RiskBrain."
    )
    parser.add_argument("--data-dir", required=True, help="Directory containing nodes.csv and edges.csv.")
    parser.add_argument("--output-dir", required=True, help="Directory to write split files and metadata.")
    parser.add_argument("--train-ratio", type=float, default=0.70)
    parser.add_argument("--val-ratio", type=float, default=0.15)
    parser.add_argument("--max-edges", type=int, default=0, help="Optional edge cap for smoke split generation.")
    parser.add_argument("--label-column", default="label_fraud")
    parser.add_argument("--event-time-column", default="event_time")
    parser.add_argument("--source-row-column", default="source_row_no")
    parser.add_argument("--source-step-column", default="source_step")
    parser.add_argument("--edge-id-column", default="edge_id")
    parser.add_argument("--from-node-column", default="from_node_id")
    parser.add_argument("--to-node-column", default="to_node_id")
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


def parse_datetime(value: str) -> Optional[datetime]:
    if not value:
        return None
    text = value.strip()
    if not text:
        return None
    for fmt in (
        "%Y-%m-%d %H:%M:%S.%f",
        "%Y-%m-%d %H:%M:%S",
        "%Y/%m/%d %H:%M:%S",
        "%Y-%m-%dT%H:%M:%S.%f",
        "%Y-%m-%dT%H:%M:%S",
    ):
        try:
            return datetime.strptime(text, fmt)
        except ValueError:
            pass
    try:
        return datetime.fromisoformat(text.replace("Z", "+00:00")).replace(tzinfo=None)
    except ValueError:
        return None


def event_order(row: Dict[str, str], row_index: int, args: argparse.Namespace) -> Tuple[float, str]:
    event_time = row.get(args.event_time_column, "")
    parsed = parse_datetime(event_time)
    if parsed is not None:
        return parsed.timestamp(), "event_time"
    source_step = row.get(args.source_step_column)
    if source_step:
        return as_float(source_step, float(row_index)), "source_step"
    source_row = row.get(args.source_row_column)
    if source_row:
        return as_float(source_row, float(row_index)), "source_row_no"
    return float(row_index), "row_index"


def label_value(row: Dict[str, str], label_column: str) -> int:
    return 1 if as_int(row.get(label_column), 0) == 1 else 0


def load_node_ids(data_dir: str) -> set:
    path = os.path.join(data_dir, "nodes.csv")
    if not os.path.exists(path):
        raise FileNotFoundError(path)
    return {as_int(row.get("node_id"), -1) for row in read_csv(path)}


def split_name_for_rank(rank: int, total: int, train_ratio: float, val_ratio: float) -> str:
    train_end = int(total * train_ratio)
    val_end = int(total * (train_ratio + val_ratio))
    if rank < train_end:
        return "train"
    if rank < val_end:
        return "val"
    return "test"


def summarize_split(records: List[Dict]) -> Dict:
    if not records:
        return {
            "count": 0,
            "positive": 0,
            "positive_ratio": 0.0,
            "event_order_min": None,
            "event_order_max": None,
            "event_time_min": None,
            "event_time_max": None,
        }
    positives = sum(item["label"] for item in records)
    timed = [item.get("event_time", "") for item in records if item.get("event_time")]
    orders = [item["event_order"] for item in records]
    return {
        "count": len(records),
        "positive": positives,
        "positive_ratio": positives / max(1, len(records)),
        "event_order_min": min(orders),
        "event_order_max": max(orders),
        "event_time_min": min(timed) if timed else None,
        "event_time_max": max(timed) if timed else None,
    }


def write_split_indices(path: str, records: List[Dict]) -> None:
    fields = [
        "model_index",
        "row_index",
        "order_rank",
        "split",
        "edge_id",
        "event_order",
        "event_order_source",
        "event_time",
        "label",
    ]
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for record in sorted(records, key=lambda item: item["model_index"]):
            writer.writerow({field: record.get(field, "") for field in fields})


def write_split_edges(output_dir: str, edge_header: List[str], records: List[Dict]) -> None:
    fieldnames = ["model_index", "row_index", "order_rank"] + edge_header
    for split in ("train", "val", "test"):
        path = os.path.join(output_dir, f"{split}_edges.csv")
        with open(path, "w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=fieldnames)
            writer.writeheader()
            for record in sorted((item for item in records if item["split"] == split),
                                 key=lambda item: item["order_rank"]):
                row = {
                    "model_index": record["model_index"],
                    "row_index": record["row_index"],
                    "order_rank": record["order_rank"],
                }
                row.update(record["row"])
                writer.writerow(row)


def build_splits(args: argparse.Namespace) -> Dict:
    if args.train_ratio <= 0 or args.val_ratio <= 0 or args.train_ratio + args.val_ratio >= 1:
        raise ValueError("train-ratio and val-ratio must be positive and leave a non-empty test split.")

    os.makedirs(args.output_dir, exist_ok=True)
    node_ids = load_node_ids(args.data_dir)
    edges_path = os.path.join(args.data_dir, "edges.csv")
    if not os.path.exists(edges_path):
        raise FileNotFoundError(edges_path)

    records: List[Dict] = []
    missing_node_edges = 0
    source_edge_rows_read = 0
    edge_header: List[str] = []

    with open(edges_path, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        edge_header = list(reader.fieldnames or [])
        required = {
            args.edge_id_column,
            args.from_node_column,
            args.to_node_column,
            args.label_column,
        }
        missing_columns = sorted(required - set(edge_header))
        if missing_columns:
            raise ValueError(f"edges.csv is missing required columns: {missing_columns}")

        for row_index, row in enumerate(reader):
            if args.max_edges > 0 and row_index >= args.max_edges:
                break
            source_edge_rows_read += 1
            from_id = as_int(row.get(args.from_node_column), -1)
            to_id = as_int(row.get(args.to_node_column), -1)
            if from_id not in node_ids or to_id not in node_ids:
                missing_node_edges += 1
                continue
            order_value, order_source = event_order(row, row_index, args)
            records.append({
                "model_index": len(records),
                "row_index": row_index,
                "edge_id": row.get(args.edge_id_column, ""),
                "event_order": order_value,
                "event_order_source": order_source,
                "event_time": row.get(args.event_time_column, ""),
                "label": label_value(row, args.label_column),
                "row": row,
            })

    ordered = sorted(records, key=lambda item: (item["event_order"], item["row_index"]))
    for rank, record in enumerate(ordered):
        record["order_rank"] = rank
        record["split"] = split_name_for_rank(rank, len(ordered), args.train_ratio, args.val_ratio)

    split_index_path = os.path.join(args.output_dir, "split_indices.csv")
    write_split_indices(split_index_path, records)
    write_split_edges(args.output_dir, edge_header, records)

    by_split = {
        split: [item for item in ordered if item["split"] == split]
        for split in ("train", "val", "test")
    }
    meta = {
        "protocol_version": "temporal-split-v1",
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "data_dir": os.path.abspath(args.data_dir),
        "output_dir": os.path.abspath(args.output_dir),
        "source_files": {
            "nodes": os.path.abspath(os.path.join(args.data_dir, "nodes.csv")),
            "edges": os.path.abspath(edges_path),
        },
        "ratios": {
            "train": args.train_ratio,
            "val": args.val_ratio,
            "test": 1.0 - args.train_ratio - args.val_ratio,
        },
        "columns": {
            "edge_id": args.edge_id_column,
            "from_node": args.from_node_column,
            "to_node": args.to_node_column,
            "event_time": args.event_time_column,
            "source_step": args.source_step_column,
            "source_row_no": args.source_row_column,
            "label": args.label_column,
        },
        "max_edges": args.max_edges,
        "source_edge_rows_read": source_edge_rows_read,
        "eligible_edges": len(records),
        "missing_node_edges": missing_node_edges,
        "total_positive": sum(item["label"] for item in records),
        "positive_ratio": (sum(item["label"] for item in records) / max(1, len(records))),
        "splits": {split: summarize_split(items) for split, items in by_split.items()},
        "split_files": {
            "indices": os.path.abspath(split_index_path),
            "train_edges": os.path.abspath(os.path.join(args.output_dir, "train_edges.csv")),
            "val_edges": os.path.abspath(os.path.join(args.output_dir, "val_edges.csv")),
            "test_edges": os.path.abspath(os.path.join(args.output_dir, "test_edges.csv")),
        },
        "recommended_training_config": {
            "feature_mode": "v3",
            "feedback_mode": "off",
            "split_mode": "time",
            "standardize_scope": "train",
            "adjacency_scope": "train",
        },
    }
    if records and not math.isfinite(meta["positive_ratio"]):
        meta["positive_ratio"] = 0.0

    with open(os.path.join(args.output_dir, "split_meta.json"), "w", encoding="utf-8") as handle:
        json.dump(meta, handle, indent=2, ensure_ascii=False)
    return meta


def main() -> None:
    args = parse_args()
    meta = build_splits(args)
    print(json.dumps({
        "protocol_version": meta["protocol_version"],
        "eligible_edges": meta["eligible_edges"],
        "total_positive": meta["total_positive"],
        "splits": meta["splits"],
        "split_file": meta["split_files"]["indices"],
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
