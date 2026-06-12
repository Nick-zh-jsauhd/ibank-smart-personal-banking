import argparse
import csv
import json
import os
from datetime import datetime, timezone
from typing import Dict, List, Tuple


REQUIRED_EDGE_COLUMNS = {
    "edge_id",
    "from_node_id",
    "to_node_id",
    "edge_type",
    "amount",
    "currency",
    "event_time",
    "label_fraud",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert iBank RiskBrain graph export into IBM Multi-GNN formatted_transactions.csv."
    )
    parser.add_argument("--edges", required=True, help="Path to RiskBrain edges.csv.")
    parser.add_argument("--split-file", required=True, help="Path to fixed temporal split_indices.csv.")
    parser.add_argument("--output-dir", required=True, help="Output dataset directory.")
    parser.add_argument("--dataset-name", default="ibank_temporal_v1")
    parser.add_argument("--limit", type=int, default=None, help="Optional row limit for smoke datasets.")
    parser.add_argument("--limit-per-split", type=int, default=None,
                        help="Optional per-split row cap for smoke datasets that still need train/val/test rows.")
    return parser.parse_args()


def read_csv(path: str) -> List[Dict[str, str]]:
    with open(path, newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def as_int(value: str, default: int = 0) -> int:
    if value is None or value == "":
        return default
    try:
        return int(float(value))
    except ValueError:
        return default


def as_float(value: str, default: float = 0.0) -> float:
    if value is None or value == "":
        return default
    try:
        return float(value)
    except ValueError:
        return default


def parse_event_time(value: str, fallback: float) -> float:
    if value:
        text = value.strip()
        for fmt in ("%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d %H:%M:%S"):
            try:
                return datetime.strptime(text, fmt).replace(tzinfo=timezone.utc).timestamp()
            except ValueError:
                pass
    return fallback


def read_split(path: str) -> Dict[int, Dict[str, object]]:
    rows = read_csv(path)
    required = {"model_index", "split", "order_rank", "event_order"}
    if not rows:
        raise ValueError("split file is empty")
    missing = required - set(rows[0].keys())
    if missing:
        raise ValueError(f"split file missing required columns: {sorted(missing)}")
    split_map = {}
    for row in rows:
        model_index = as_int(row.get("model_index"), -1)
        split = row.get("split", "")
        if split not in {"train", "val", "test"}:
            raise ValueError(f"invalid split value for model_index={model_index}: {split}")
        split_map[model_index] = {
            "split": split,
            "order_rank": as_int(row.get("order_rank"), model_index),
            "event_order": as_float(row.get("event_order"), 0.0),
            "edge_id": row.get("edge_id", ""),
        }
    return split_map


def dense_id(value: str, mapping: Dict[str, int]) -> int:
    key = str(value)
    if key not in mapping:
        mapping[key] = len(mapping)
    return mapping[key]


def validate_edges(rows: List[Dict[str, str]]) -> None:
    if not rows:
        raise ValueError("edges file is empty")
    missing = REQUIRED_EDGE_COLUMNS - set(rows[0].keys())
    if missing:
        raise ValueError(f"edges file missing required columns: {sorted(missing)}")


def ordered_edges(edges: List[Dict[str, str]], split_map: Dict[int, Dict[str, object]],
                  limit: int = None, limit_per_split: int = None) -> List[Tuple[int, Dict[str, str], Dict[str, object]]]:
    if len(edges) != len(split_map):
        raise ValueError(f"edge row count and split row count differ: {len(edges)} vs {len(split_map)}")
    ordered = []
    for model_index, row in enumerate(edges):
        if model_index not in split_map:
            raise ValueError(f"split file missing model_index={model_index}")
        split = split_map[model_index]
        ordered.append((model_index, row, split))
    ordered.sort(key=lambda item: (int(item[2]["order_rank"]), model_index_for_sort(item)))
    if limit_per_split is not None:
        by_split = {"train": [], "val": [], "test": []}
        for item in ordered:
            by_split[str(item[2]["split"])].append(item)
        capped = []
        for split in ["train", "val", "test"]:
            items = by_split[split]
            if len(items) <= limit_per_split:
                capped.extend(items)
                continue
            if limit_per_split <= 1:
                capped.append(items[0])
                continue
            step = (len(items) - 1) / float(limit_per_split - 1)
            selected_indexes = sorted({round(i * step) for i in range(limit_per_split)})
            for index in selected_indexes:
                capped.append(items[index])
        ordered = capped
    if limit is not None:
        ordered = ordered[:limit]
    return ordered


def model_index_for_sort(item: Tuple[int, Dict[str, str], Dict[str, object]]) -> int:
    return item[0]


def write_outputs(args: argparse.Namespace) -> Dict[str, object]:
    edges = read_csv(args.edges)
    validate_edges(edges)
    split_map = read_split(args.split_file)
    ordered = ordered_edges(edges, split_map, args.limit, args.limit_per_split)
    os.makedirs(args.output_dir, exist_ok=True)

    account_ids: Dict[str, int] = {}
    currency_ids: Dict[str, int] = {}
    payment_format_ids: Dict[str, int] = {}
    split_counts = {"train": 0, "val": 0, "test": 0}
    label_counts = {"train": 0, "val": 0, "test": 0}
    row_map_path = os.path.join(args.output_dir, "ibank_row_map.csv")
    split_path = os.path.join(args.output_dir, "ibank_external_split.csv")
    formatted_path = os.path.join(args.output_dir, "formatted_transactions.csv")

    with open(formatted_path, "w", newline="", encoding="utf-8") as formatted_handle, \
            open(split_path, "w", newline="", encoding="utf-8") as split_handle, \
            open(row_map_path, "w", newline="", encoding="utf-8") as map_handle:
        formatted_writer = csv.DictWriter(formatted_handle, fieldnames=[
            "EdgeID",
            "from_id",
            "to_id",
            "Timestamp",
            "Amount Sent",
            "Sent Currency",
            "Amount Received",
            "Received Currency",
            "Payment Format",
            "Is Laundering",
        ])
        split_writer = csv.DictWriter(split_handle, fieldnames=[
            "row_position",
            "model_index",
            "split",
            "order_rank",
            "edge_id",
            "label",
        ])
        map_writer = csv.DictWriter(map_handle, fieldnames=[
            "row_position",
            "model_index",
            "edge_id",
            "source_row_no",
            "from_node_id",
            "to_node_id",
            "dense_from_id",
            "dense_to_id",
            "event_time",
            "event_order",
            "split",
            "label",
        ])
        formatted_writer.writeheader()
        split_writer.writeheader()
        map_writer.writeheader()

        for row_position, (model_index, row, split_meta) in enumerate(ordered):
            split = str(split_meta["split"])
            label = 1 if as_int(row.get("label_fraud"), 0) == 1 else 0
            split_counts[split] += 1
            label_counts[split] += label
            event_order = as_float(str(split_meta.get("event_order", "")), float(row_position))
            timestamp = int(parse_event_time(row.get("event_time", ""), event_order))
            currency_id = dense_id(row.get("currency", ""), currency_ids)
            payment_format_id = dense_id(row.get("edge_type", ""), payment_format_ids)
            from_id = dense_id(row.get("from_node_id", ""), account_ids)
            to_id = dense_id(row.get("to_node_id", ""), account_ids)
            amount = as_float(row.get("amount"), 0.0)
            edge_id = row.get("edge_id", str(model_index))

            formatted_writer.writerow({
                "EdgeID": model_index,
                "from_id": from_id,
                "to_id": to_id,
                "Timestamp": timestamp,
                "Amount Sent": f"{amount:.8f}",
                "Sent Currency": currency_id,
                "Amount Received": f"{amount:.8f}",
                "Received Currency": currency_id,
                "Payment Format": payment_format_id,
                "Is Laundering": label,
            })
            split_writer.writerow({
                "row_position": row_position,
                "model_index": model_index,
                "split": split,
                "order_rank": split_meta.get("order_rank", row_position),
                "edge_id": edge_id,
                "label": label,
            })
            map_writer.writerow({
                "row_position": row_position,
                "model_index": model_index,
                "edge_id": edge_id,
                "source_row_no": row.get("source_row_no", ""),
                "from_node_id": row.get("from_node_id", ""),
                "to_node_id": row.get("to_node_id", ""),
                "dense_from_id": from_id,
                "dense_to_id": to_id,
                "event_time": row.get("event_time", ""),
                "event_order": event_order,
                "split": split,
                "label": label,
            })

    meta = {
        "dataset_name": args.dataset_name,
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "source_edges": os.path.abspath(args.edges),
        "source_split_file": os.path.abspath(args.split_file),
        "output_dir": os.path.abspath(args.output_dir),
        "formatted_transactions": os.path.abspath(formatted_path),
        "external_split": os.path.abspath(split_path),
        "row_map": os.path.abspath(row_map_path),
        "row_count": len(ordered),
        "account_count": len(account_ids),
        "currency_count": len(currency_ids),
        "payment_format_count": len(payment_format_ids),
        "split_counts": split_counts,
        "label_counts": label_counts,
        "split_positive_rate": {
            split: (label_counts[split] / split_counts[split] if split_counts[split] else 0.0)
            for split in split_counts
        },
        "edge_id_semantics": "EdgeID is RiskBrain model_index; ibank_row_map.csv maps model_index to graph_edge_id.",
        "row_order": "sorted by fixed temporal split order_rank; train/val/test positions are contiguous for Multi-GNN loaders.",
    }
    with open(os.path.join(args.output_dir, "ibank_multignn_meta.json"), "w", encoding="utf-8") as handle:
        json.dump(meta, handle, indent=2, ensure_ascii=False)
    return meta


def main() -> None:
    args = parse_args()
    meta = write_outputs(args)
    print(json.dumps(meta, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
