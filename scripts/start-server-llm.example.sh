#!/usr/bin/env bash
set -euo pipefail

# Example script for a private GPU server.
# Do not commit a real server path, model hash, token, or host-specific value.

BASE_DIR="${IBANK_LLM_BASE_DIR:-/opt/ibank-llm}"
MODEL_PATH="${IBANK_LLM_MODEL_PATH:-/models/qwen3-8b-q4.gguf}"
HOST="${IBANK_LLM_HOST:-127.0.0.1}"
PORT="${IBANK_LLM_PORT:-8000}"
CTX_SIZE="${IBANK_LLM_CTX_SIZE:-8192}"
GPU_LAYERS="${IBANK_LLM_GPU_LAYERS:-99}"

cd "$BASE_DIR/llama.cpp"

./build/bin/llama-server \
  --host "$HOST" \
  --port "$PORT" \
  --model "$MODEL_PATH" \
  --ctx-size "$CTX_SIZE" \
  --n-gpu-layers "$GPU_LAYERS"
