#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-/root/autodl-tmp/ibank-riskbrain}"
PY="${PY:-/root/miniconda3/bin/python}"
PAPER_DIR="${PAPER_DIR:-$BASE/external/Multi-GNN-paper}"
OUTPUT_DIR="${OUTPUT_DIR:-$BASE/outputs/multignn_paper_repro}"
DATASET_NAME="${DATASET_NAME:-IBM_AML_HI_SMALL_FULL_RAW}"
EPOCHS="${EPOCHS:-100}"
BATCH_SIZE="${BATCH_SIZE:-8192}"
SEEDS="${SEEDS:-1}"
RUNS="${RUNS:-gin gin_multi pna pna_multi}"

mkdir -p "$OUTPUT_DIR"
cd "$PAPER_DIR"

run_one() {
  local name="$1"
  local model="$2"
  local seed="$3"
  shift 3
  local log="$OUTPUT_DIR/${name}_seed${seed}_${EPOCHS}epoch.log"
  echo "Running $name seed=$seed epochs=$EPOCHS -> $log"
  "$PY" main.py \
    --data "$DATASET_NAME" \
    --model "$model" \
    --seed "$seed" \
    --n_epochs "$EPOCHS" \
    --batch_size "$BATCH_SIZE" \
    --testing \
    "$@" 2>&1 | tee "$log"
}

for seed in $SEEDS; do
  for run in $RUNS; do
    case "$run" in
      gin)
        run_one "gin" "gin" "$seed"
        ;;
      gin_multi)
        run_one "gin_multi" "gin" "$seed" --emlps --reverse_mp --ego --ports
        ;;
      pna)
        run_one "pna" "pna" "$seed"
        ;;
      pna_multi)
        run_one "pna_multi" "pna" "$seed" --emlps --reverse_mp --ego --ports
        ;;
      *)
        echo "Unknown RUNS entry: $run" >&2
        exit 1
        ;;
    esac
  done
done
