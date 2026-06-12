#!/usr/bin/env python3
"""Summarize IBM/Multi-GNN training logs by validation F1."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


F1_RE = re.compile(r"(Train|Validation|Test) F1: ([0-9.]+)")


def summarize_log(path: Path) -> dict:
    epochs: list[dict] = []
    current: dict[str, float] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        match = F1_RE.search(line)
        if not match:
            continue
        key = match.group(1).lower()
        current[f"{key}_f1"] = float(match.group(2))
        if key == "test":
            epochs.append({"epoch": len(epochs) + 1, **current})
            current = {}

    best_by_val = max(epochs, key=lambda row: row.get("validation_f1", float("-inf")), default=None)
    best_by_test = max(epochs, key=lambda row: row.get("test_f1", float("-inf")), default=None)
    return {
        "log": str(path),
        "epochs_observed": len(epochs),
        "best_by_validation_f1": best_by_val,
        "best_observed_test_f1": best_by_test,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--output", type=Path, default=None)
    args = parser.parse_args()

    summary = [summarize_log(path) for path in args.logs]
    text = json.dumps(summary, indent=2)
    if args.output:
        args.output.write_text(text, encoding="utf-8")
    print(text)


if __name__ == "__main__":
    main()
