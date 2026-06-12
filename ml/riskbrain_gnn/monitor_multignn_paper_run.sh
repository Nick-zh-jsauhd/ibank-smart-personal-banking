#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-/root/autodl-tmp/ibank-riskbrain}"
PY="${PY:-/root/miniconda3/bin/python}"
RUN_LOG="${RUN_LOG:-$BASE/outputs/multignn_paper_repro/gin_multi_seed1_100epoch.log}"
SUMMARY_JSON="${SUMMARY_JSON:-$BASE/outputs/multignn_paper_repro/gin_multi_seed1_100epoch.current_summary.json}"
MONITOR_LOG="${MONITOR_LOG:-$BASE/outputs/multignn_paper_repro/gin_multi_seed1_100epoch.monitor.log}"
INTERVAL_SECONDS="${INTERVAL_SECONDS:-600}"

while true; do
  {
    echo "===== $(date '+%F %T') ====="
    pgrep -af 'run_multignn_paper_matrix|main.py --data IBM_AML_HI_SMALL_FULL_RAW' || true
    ps -o pid,etime,pcpu,pmem,rss,cmd -p "$(pgrep -f 'main.py --data IBM_AML_HI_SMALL_FULL_RAW' | head -1)" 2>/dev/null || true
    nvidia-smi --query-gpu=utilization.gpu,memory.used,memory.total --format=csv,noheader || true
    "$PY" "$BASE/scripts/summarize_multignn_log.py" "$RUN_LOG" --output "$SUMMARY_JSON" || true
    tail -12 "$RUN_LOG" || true
    echo
  } >> "$MONITOR_LOG" 2>&1

  if ! pgrep -f 'main.py --data IBM_AML_HI_SMALL_FULL_RAW' >/dev/null; then
    echo "===== $(date '+%F %T') training process exited =====" >> "$MONITOR_LOG"
    break
  fi
  sleep "$INTERVAL_SECONDS"
done
