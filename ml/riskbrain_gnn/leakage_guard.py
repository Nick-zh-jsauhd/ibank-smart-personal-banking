import argparse
import csv
import json
import os
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Optional


LABEL_TERMS = (
    "label",
    "fraud",
    "launder",
    "laundering",
    "typology",
    "sar",
    "suspicious",
    "alert",
    "illicit",
    "review_result",
    "feedback",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Check iBank RiskBrain data and training metadata for common leakage paths."
    )
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--split-file", default=None)
    parser.add_argument("--metrics-file", default=None)
    parser.add_argument("--output-dir", default=None)
    parser.add_argument("--feature-mode", choices=["v1", "v2", "v3", "temporal_v1", "line_temporal_v1", "line_neural_v1", "line_neural_v2"], default="v3")
    parser.add_argument("--feedback-mode", choices=["off", "weight", "label"], default="off")
    parser.add_argument("--split-mode", choices=["stratified", "time"], default="time")
    parser.add_argument("--standardize-scope", choices=["full", "train"], default="train")
    parser.add_argument("--adjacency-scope", choices=["full", "train", "temporal_past"], default="train")
    parser.add_argument("--warn-only", action="store_true", help="Always exit 0 after writing the report.")
    return parser.parse_args()


def read_header(path: str) -> List[str]:
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as handle:
        reader = csv.reader(handle)
        return next(reader, [])


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


def add(checks: List[Dict], level: str, code: str, message: str, details=None) -> None:
    checks.append({
        "level": level,
        "code": code,
        "message": message,
        "details": details or {},
    })


def label_like_columns(columns: List[str]) -> List[str]:
    result = []
    for column in columns:
        lower = column.lower()
        if any(term in lower for term in LABEL_TERMS):
            result.append(column)
    return result


def check_protocol(args: argparse.Namespace, checks: List[Dict]) -> None:
    expected = {
        "feature_mode": "v3",
        "feedback_mode": "off",
        "split_mode": "time",
        "standardize_scope": "train",
        "adjacency_scope": "train",
    }
    actual = {
        "feature_mode": args.feature_mode,
        "feedback_mode": args.feedback_mode,
        "split_mode": args.split_mode,
        "standardize_scope": args.standardize_scope,
        "adjacency_scope": args.adjacency_scope,
    }
    safe_feature_modes = {"v3", "temporal_v1", "line_temporal_v1", "line_neural_v1", "line_neural_v2"}
    for key, expected_value in expected.items():
        actual_value = actual[key]
        if key == "feature_mode":
            if actual_value in safe_feature_modes:
                add(checks, "PASS", f"protocol.{key}", f"{key} is leakage-safe: {actual_value}.")
            else:
                add(checks, "FAIL", f"protocol.{key}",
                    f"{key}={actual_value} is not a strict leakage-safe feature mode.",
                    {"expected_one_of": sorted(safe_feature_modes), "actual": actual_value})
            continue
        if key == "adjacency_scope" and args.feature_mode in {"line_neural_v1", "line_neural_v2"}:
            if actual_value == "temporal_past":
                add(checks, "PASS", f"protocol.{key}",
                    f"adjacency_scope=temporal_past is leakage-safe for {args.feature_mode} because messages only flow from prior transactions.")
            else:
                add(checks, "FAIL", f"protocol.{key}",
                    f"{key}={actual_value} is not the strict line-neural temporal setting.",
                    {"expected": "temporal_past", "actual": actual_value})
            continue
        if actual_value == expected_value:
            add(checks, "PASS", f"protocol.{key}", f"{key} is leakage-safe: {actual_value}.")
        else:
            add(checks, "FAIL", f"protocol.{key}",
                f"{key}={actual_value} is not the strict leakage-safe setting.",
                {"expected": expected_value, "actual": actual_value})


def check_headers(args: argparse.Namespace, checks: List[Dict]) -> None:
    nodes = read_header(os.path.join(args.data_dir, "nodes.csv"))
    edges = read_header(os.path.join(args.data_dir, "edges.csv"))
    edge_features = read_header(os.path.join(args.data_dir, "edge_features_v2.csv"))

    node_label_like = label_like_columns(nodes)
    if node_label_like:
        if args.feature_mode == "v1":
            add(checks, "FAIL", "headers.nodes.label_like",
                "node feature input contains label-like columns and feature_mode=v1 can consume fraud-degree columns.",
                {"columns": node_label_like})
        else:
            add(checks, "WARN", "headers.nodes.label_like",
                "raw nodes.csv contains label-like columns; current feature mode does not consume them.",
                {"columns": node_label_like, "feature_mode": args.feature_mode})
    else:
        add(checks, "PASS", "headers.nodes.label_like", "nodes.csv has no label-like columns.")

    edge_label_like = label_like_columns(edges)
    allowed_target_columns = {"label_fraud", "label_rule", "label_source", "typology"}
    unexpected_edge_columns = sorted(set(edge_label_like) - allowed_target_columns)
    if unexpected_edge_columns:
        add(checks, "FAIL", "headers.edges.unexpected_label_like",
            "edges.csv contains unexpected label-like metadata columns.",
            {"columns": unexpected_edge_columns})
    else:
        add(checks, "PASS", "headers.edges.target_columns",
            "edges.csv label-like columns are limited to target/audit metadata.",
            {"columns": edge_label_like})

    edge_feature_label_like = label_like_columns(edge_features)
    if edge_feature_label_like:
        if args.feedback_mode == "off":
            add(checks, "WARN", "headers.edge_features.feedback_columns",
                "edge_features_v2.csv contains review/feedback columns; feedback_mode=off keeps them out of labels and weights.",
                {"columns": edge_feature_label_like})
        else:
            add(checks, "FAIL", "headers.edge_features.feedback_columns",
                "review/feedback columns can leak human decisions when feedback_mode is not off.",
                {"columns": edge_feature_label_like, "feedback_mode": args.feedback_mode})
    elif edge_features:
        add(checks, "PASS", "headers.edge_features.label_like", "edge_features_v2.csv has no label-like columns.")
    else:
        add(checks, "PASS", "headers.edge_features.absent", "edge_features_v2.csv is absent; no feedback leakage path.")


def check_split_file(split_file: str, checks: List[Dict]) -> Dict:
    if not split_file:
        add(checks, "WARN", "split.file.absent",
            "No external split file was provided; training must use an internal chronological split.")
        return {}
    if not os.path.exists(split_file):
        add(checks, "FAIL", "split.file.missing", "split file does not exist.", {"path": split_file})
        return {}

    required = {"model_index", "split", "event_order", "label"}
    rows = []
    with open(split_file, newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        missing = sorted(required - set(reader.fieldnames or []))
        if missing:
            add(checks, "FAIL", "split.columns.missing",
                "split file is missing required columns.", {"missing": missing})
            return {}
        for row in reader:
            rows.append(row)

    by_split = {"train": [], "val": [], "test": []}
    seen = set()
    duplicate_indexes = []
    invalid_splits = []
    for row in rows:
        model_index = as_int(row.get("model_index"), -1)
        split = row.get("split", "")
        if model_index in seen:
            duplicate_indexes.append(model_index)
        seen.add(model_index)
        if split not in by_split:
            invalid_splits.append(split)
            continue
        by_split[split].append(row)

    if duplicate_indexes:
        add(checks, "FAIL", "split.index.duplicate",
            "split file contains duplicate model_index values.",
            {"examples": duplicate_indexes[:10]})
    else:
        add(checks, "PASS", "split.index.disjoint", "split assignments are disjoint by model_index.")

    if invalid_splits:
        add(checks, "FAIL", "split.name.invalid",
            "split file contains invalid split names.", {"examples": sorted(set(invalid_splits))})
    else:
        add(checks, "PASS", "split.name.valid", "split names are limited to train, val, test.")

    summary = {}
    for split, items in by_split.items():
        labels = [as_int(item.get("label"), 0) for item in items]
        orders = [as_float(item.get("event_order"), 0.0) for item in items]
        summary[split] = {
            "count": len(items),
            "positive": sum(1 for value in labels if value == 1),
            "positive_ratio": sum(1 for value in labels if value == 1) / max(1, len(items)),
            "event_order_min": min(orders) if orders else None,
            "event_order_max": max(orders) if orders else None,
        }
        if len(items) == 0:
            add(checks, "FAIL", f"split.{split}.empty", f"{split} split is empty.")
        elif summary[split]["positive"] == 0:
            add(checks, "WARN", f"split.{split}.no_positive",
                f"{split} split contains no positive labels.", summary[split])
        else:
            add(checks, "PASS", f"split.{split}.positive",
                f"{split} split has positive labels.", summary[split])

    train_max = summary["train"]["event_order_max"]
    val_min = summary["val"]["event_order_min"]
    val_max = summary["val"]["event_order_max"]
    test_min = summary["test"]["event_order_min"]
    if train_max is not None and val_min is not None and val_max is not None and test_min is not None:
        if train_max <= val_min and val_max <= test_min:
            add(checks, "PASS", "split.temporal_order",
                "train, validation, and test windows are chronologically ordered.")
        else:
            add(checks, "FAIL", "split.temporal_order",
                "split windows overlap or are not chronologically ordered.",
                {"train_max": train_max, "val_min": val_min, "val_max": val_max, "test_min": test_min})
    return summary


def check_metrics(metrics_file: Optional[str], args: argparse.Namespace, checks: List[Dict]) -> Dict:
    if not metrics_file:
        add(checks, "WARN", "metrics.file.absent",
            "No metrics.json was provided; only data/protocol checks were run.")
        return {}
    if not os.path.exists(metrics_file):
        add(checks, "FAIL", "metrics.file.missing", "metrics.json does not exist.", {"path": metrics_file})
        return {}
    with open(metrics_file, encoding="utf-8") as handle:
        metrics = json.load(handle)
    meta = metrics.get("meta", {})
    for key, expected in {
        "feature_mode": args.feature_mode,
        "feedback_mode": args.feedback_mode,
        "split_mode": args.split_mode,
        "standardize_scope": args.standardize_scope,
        "adjacency_scope": args.adjacency_scope,
    }.items():
        actual = meta.get(key)
        if actual == expected:
            add(checks, "PASS", f"metrics.meta.{key}", f"metrics meta confirms {key}={actual}.")
        else:
            add(checks, "FAIL", f"metrics.meta.{key}",
                f"metrics meta does not match requested protocol for {key}.",
                {"expected": expected, "actual": actual})
    normalization = meta.get("normalization", {})
    if normalization.get("scope") == "train":
        add(checks, "PASS", "metrics.normalization.train",
            "checkpoint metadata records train-only normalization.")
    elif args.feature_mode in {"temporal_v1", "line_temporal_v1"} and normalization.get("scope") == "not_required_tree_model":
        add(checks, "PASS", "metrics.normalization.not_required",
            "temporal tree baseline records that no feature scaler is fit.")
    else:
        add(checks, "FAIL", "metrics.normalization.train",
            "checkpoint metadata does not record train-only normalization.",
            {"normalization": normalization})
    return metrics


def report_markdown(report: Dict) -> str:
    lines = [
        "# RiskBrain Leakage Guard Report",
        "",
        f"- Created: {report['created_at_utc']}",
        f"- Data dir: `{report['data_dir']}`",
        f"- Status: **{report['status']}**",
        "",
        "## Checks",
        "",
    ]
    for check in report["checks"]:
        details = check.get("details") or {}
        lines.append(f"- **{check['level']}** `{check['code']}`: {check['message']}")
        if details:
            lines.append(f"  - details: `{json.dumps(details, ensure_ascii=False)}`")
    if report.get("split_summary"):
        lines.extend(["", "## Split Summary", ""])
        for split, summary in report["split_summary"].items():
            lines.append(
                f"- `{split}`: count={summary['count']}, positive={summary['positive']}, "
                f"positive_ratio={summary['positive_ratio']:.6f}"
            )
    return "\n".join(lines) + "\n"


def run_guard(args: argparse.Namespace) -> Dict:
    checks: List[Dict] = []
    check_protocol(args, checks)
    check_headers(args, checks)
    split_summary = check_split_file(args.split_file, checks)
    metrics = check_metrics(args.metrics_file, args, checks)

    fail_count = sum(1 for check in checks if check["level"] == "FAIL")
    warn_count = sum(1 for check in checks if check["level"] == "WARN")
    status = "FAIL" if fail_count else "PASS_WITH_WARNINGS" if warn_count else "PASS"
    report = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "data_dir": os.path.abspath(args.data_dir),
        "split_file": os.path.abspath(args.split_file) if args.split_file else None,
        "metrics_file": os.path.abspath(args.metrics_file) if args.metrics_file else None,
        "requested_protocol": {
            "feature_mode": args.feature_mode,
            "feedback_mode": args.feedback_mode,
            "split_mode": args.split_mode,
            "standardize_scope": args.standardize_scope,
            "adjacency_scope": args.adjacency_scope,
        },
        "status": status,
        "fail_count": fail_count,
        "warn_count": warn_count,
        "checks": checks,
        "split_summary": split_summary,
        "metrics_test": metrics.get("test_metrics", {}) if metrics else {},
    }
    return report


def main() -> None:
    args = parse_args()
    report = run_guard(args)
    output_dir = args.output_dir or os.path.join(args.data_dir, "leakage_guard")
    os.makedirs(output_dir, exist_ok=True)
    json_path = os.path.join(output_dir, "leakage_report.json")
    md_path = os.path.join(output_dir, "leakage_report.md")
    with open(json_path, "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2, ensure_ascii=False)
    with open(md_path, "w", encoding="utf-8") as handle:
        handle.write(report_markdown(report))
    print(json.dumps({
        "status": report["status"],
        "fail_count": report["fail_count"],
        "warn_count": report["warn_count"],
        "json_report": os.path.abspath(json_path),
        "markdown_report": os.path.abspath(md_path),
    }, indent=2, ensure_ascii=False))
    if report["fail_count"] and not args.warn_only:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
