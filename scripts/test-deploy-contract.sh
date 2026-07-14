#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT/deploy/deploy.sh"

deploy_command="$(grep -E 'docker-compose .* up -d ' "$DEPLOY_SCRIPT" || true)"
if [[ -z "$deploy_command" ]]; then
  echo "错误：部署脚本缺少 docker-compose 启动命令" >&2
  exit 1
fi

if [[ "$deploy_command" != *"--force-recreate backend nginx"* ]]; then
  echo "错误：部署脚本必须使用 --force-recreate backend nginx 强制重启生产服务" >&2
  exit 1
fi

echo "部署脚本重启契约通过"
