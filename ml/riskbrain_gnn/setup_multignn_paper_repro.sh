#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-/root/autodl-tmp/ibank-riskbrain}"
PY="${PY:-/root/miniconda3/bin/python}"
RAW_DIR="${RAW_DIR:-$BASE/data/IBM_AML_HI_SMALL_FULL_RAW}"
RAW_ZIP="${RAW_ZIP:-$RAW_DIR/HI-Small_Trans.csv.zip}"
RAW_CSV="${RAW_CSV:-$RAW_DIR/HI-Small_Trans.csv}"
PAPER_DIR="${PAPER_DIR:-$BASE/external/Multi-GNN-paper}"
PAPER_TMP="${PAPER_TMP:-$BASE/external/multignn-paper-tmp}"
PAPER_MODEL_DIR="${PAPER_MODEL_DIR:-$BASE/outputs/multignn_paper_models}"
PAPER_OUTPUT_DIR="${PAPER_OUTPUT_DIR:-$BASE/outputs/multignn_paper_repro}"
DATASET_NAME="${DATASET_NAME:-IBM_AML_HI_SMALL_FULL_RAW}"
FALLBACK_FORMATTER="${FALLBACK_FORMATTER:-$BASE/scripts/format_ibm_aml_for_multignn.py}"

if [[ ! -f "$BASE/external/Multi-GNN.zip" ]]; then
  echo "Missing official Multi-GNN zip: $BASE/external/Multi-GNN.zip" >&2
  exit 1
fi

if [[ ! -f "$RAW_ZIP" && ! -f "$RAW_CSV" ]]; then
  echo "Missing raw HI-Small file. Expected $RAW_ZIP or $RAW_CSV" >&2
  exit 1
fi

mkdir -p "$RAW_DIR" "$PAPER_MODEL_DIR" "$PAPER_OUTPUT_DIR"
"$PY" -m pip install -q datatable

rm -rf "$PAPER_DIR" "$PAPER_TMP"
unzip -q "$BASE/external/Multi-GNN.zip" -d "$PAPER_TMP"
mv "$PAPER_TMP/Multi-GNN-main" "$PAPER_DIR"

export BASE PAPER_DIR PAPER_MODEL_DIR
"$PY" <<'PY'
from pathlib import Path
import json
import os

paper_dir = Path(os.environ["PAPER_DIR"])
model_dir = os.environ["PAPER_MODEL_DIR"]
config = {
    "paths": {
        "aml_data": f"{os.environ['BASE']}/data",
        "model_to_load": model_dir,
        "model_to_save": model_dir,
    }
}
(paper_dir / "data_config.json").write_text(json.dumps(config, indent=2), encoding="utf-8")

# PyG 2.x BaseTransform dispatches through forward(). This is a compatibility
# fix only; the data split, features, sampling and loss protocol remain official.
train_util = paper_dir / "train_util.py"
text = train_util.read_text(encoding="utf-8")
text = text.replace(
    "    def __call__(self, data: Union[Data, HeteroData]):",
    "    def forward(self, data: Union[Data, HeteroData]):",
)
train_util.write_text(text, encoding="utf-8")
print(f"Prepared paper protocol copy: {paper_dir}")
PY

if [[ ! -f "$RAW_CSV" ]]; then
  unzip -o -q "$RAW_ZIP" -d "$RAW_DIR"
fi

cd "$PAPER_DIR"
if ! "$PY" format_kaggle_files.py "$RAW_CSV"; then
  echo "Official datatable formatter failed; using pandas-compatible formatter: $FALLBACK_FORMATTER" >&2
  "$PY" "$FALLBACK_FORMATTER" "$RAW_CSV"
fi

export RAW_CSV FORMATTED_CSV="$RAW_DIR/formatted_transactions.csv" PAPER_OUTPUT_DIR DATASET_NAME
"$PY" <<'PY'
import csv
import json
import os
from pathlib import Path

raw_csv = Path(os.environ["RAW_CSV"])
formatted_csv = Path(os.environ["FORMATTED_CSV"])
out_dir = Path(os.environ["PAPER_OUTPUT_DIR"])
out_dir.mkdir(parents=True, exist_ok=True)

def count_labels(path: Path, label_column: str):
    total = 0
    positives = 0
    first_ts = None
    last_ts = None
    with path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            total += 1
            positives += int(row[label_column])
            ts = row.get("Timestamp")
            if first_ts is None:
                first_ts = ts
            last_ts = ts
    return {
        "rows": total,
        "positives": positives,
        "positive_rate": positives / total if total else 0.0,
        "first_timestamp": first_ts,
        "last_timestamp": last_ts,
    }

meta = {
    "dataset_name": os.environ["DATASET_NAME"],
    "raw_csv": str(raw_csv),
    "formatted_csv": str(formatted_csv),
    "raw": count_labels(raw_csv, "Is Laundering"),
    "formatted": count_labels(formatted_csv, "Is Laundering"),
    "protocol": {
        "codebase": "IBM/Multi-GNN official copy with PyG BaseTransform compatibility patch",
        "split": "official temporal day-based 60/20/20 from data_loading.py",
        "metrics_primary": "minority-class F1 selected by best validation F1",
        "num_neighbors": [100, 100],
        "epochs": 100,
    },
}
(out_dir / "hi_small_full_dataset_meta.json").write_text(
    json.dumps(meta, indent=2), encoding="utf-8"
)
print(json.dumps(meta, indent=2))
PY

ls -lh "$RAW_CSV" "$RAW_DIR/formatted_transactions.csv"
