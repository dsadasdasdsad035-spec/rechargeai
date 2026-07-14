#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT/deploy/deploy.sh"
COMPOSE_FILE="$ROOT/deploy/docker-compose.prod.yml"

fail() {
  echo "错误：$1" >&2
  exit 1
}

command_line() {
  local pattern="$1"
  grep -nE "$pattern" "$DEPLOY_SCRIPT" | head -n 1 | cut -d: -f1 || true
}

bash -n "$DEPLOY_SCRIPT" || fail "部署脚本存在 Bash 语法错误"

backend_block="$(awk '
  /^  backend:$/ { capture = 1 }
  capture && /^  [a-zA-Z0-9_-]+:$/ && $0 != "  backend:" { exit }
  capture { print }
' "$COMPOSE_FILE")"
[[ -n "$backend_block" ]] || fail "生产 Compose 缺少 backend 服务"
grep -qE '^    healthcheck:$' <<<"$backend_block" || fail "backend 服务缺少 healthcheck"
grep -q 'wget' <<<"$backend_block" || fail "backend healthcheck 必须使用镜像内置 wget"
grep -q '/actuator/health' <<<"$backend_block" || fail "backend healthcheck 未访问健康端点"
grep -q '"status":"UP"' <<<"$backend_block" || fail "backend healthcheck 未校验 UP 状态"

backend_restart_line="$(command_line '^[[:space:]]*remote_exec "docker-compose -f docker-compose\.prod\.yml up -d --no-deps --no-build --force-recreate backend"[[:space:]]*$')"
backend_wait_line="$(command_line '^[[:space:]]*wait_for_backend_health[[:space:]]*$')"
nginx_restart_line="$(command_line '^[[:space:]]*remote_exec "docker-compose -f docker-compose\.prod\.yml up -d --no-deps --no-build --force-recreate nginx"[[:space:]]*$')"
public_wait_line="$(command_line '^[[:space:]]*wait_for_public_health[[:space:]]*$')"
state_verify_line="$(command_line '^[[:space:]]*verify_container_state[[:space:]]*$')"

[[ -n "$backend_restart_line" ]] || fail "缺少真实 backend 强制重建命令"
[[ -n "$backend_wait_line" ]] || fail "缺少 wait_for_backend_health 调用"
[[ -n "$nginx_restart_line" ]] || fail "缺少真实 nginx 强制重建命令"
[[ -n "$public_wait_line" ]] || fail "缺少 wait_for_public_health 调用"
[[ -n "$state_verify_line" ]] || fail "缺少 verify_container_state 调用"

if ! ((backend_restart_line < backend_wait_line
    && backend_wait_line < nginx_restart_line
    && nginx_restart_line < public_wait_line
    && public_wait_line < state_verify_line)); then
  fail "服务重建、健康等待与状态核验顺序不正确"
fi

local_sha_line="$(command_line '^[[:space:]]*LOCAL_JAR_SHA256=.*sha256_file')"
remote_sha_line="$(command_line '^[[:space:]]*REMOTE_JAR_SHA256=.*remote_exec.*sha256sum')"
sha_compare_line="$(command_line '^[[:space:]]*if \[\[ "\$LOCAL_JAR_SHA256" != "\$REMOTE_JAR_SHA256" \]\]; then[[:space:]]*$')"

[[ -n "$local_sha_line" ]] || fail "缺少本地 JAR SHA256 计算"
[[ -n "$remote_sha_line" ]] || fail "缺少远端 JAR SHA256 计算"
[[ -n "$sha_compare_line" ]] || fail "缺少 JAR SHA256 不一致失败分支"
if ! ((local_sha_line < remote_sha_line
    && remote_sha_line < sha_compare_line
    && sha_compare_line < backend_restart_line)); then
  fail "JAR SHA256 必须在重建 backend 前完成比对"
fi

grep -qE '^wait_for_backend_health\(\)[[:space:]]*\{' "$DEPLOY_SCRIPT" \
  || fail "缺少 wait_for_backend_health 函数"
grep -qE '^wait_for_public_health\(\)[[:space:]]*\{' "$DEPLOY_SCRIPT" \
  || fail "缺少 wait_for_public_health 函数"
grep -qE '^[[:space:]]*local health_url=.*actuator/health' "$DEPLOY_SCRIPT" \
  || fail "公网健康等待未定义 actuator 健康地址"
grep -qE '^[[:space:]]*response=.*remote_exec "curl .*--resolve .*127\.0\.0\.1.*\$health_url' "$DEPLOY_SCRIPT" \
  || fail "公网健康检查必须通过 SSH 在远端回环地址执行"
if grep -qE '^[[:space:]]*response="\$\(curl .*\$health_url' "$DEPLOY_SCRIPT"; then
  fail "不得从部署机直接请求受限的生产 actuator 端点"
fi
grep -qE '^verify_container_state\(\)[[:space:]]*\{' "$DEPLOY_SCRIPT" \
  || fail "缺少 verify_container_state 函数"
grep -qE '^[[:space:]]*health_status=.*remote_exec ".*docker inspect -f .*State\.Health\.Status.*wildai-backend' "$DEPLOY_SCRIPT" \
  || fail "后端等待未读取容器 Health.Status"
grep -qE '^[[:space:]]*container_state=.*remote_exec ".*docker inspect -f .*State\.Running.*State\.StartedAt' "$DEPLOY_SCRIPT" \
  || fail "容器状态核验缺少 Running 或 StartedAt"

echo "部署脚本就绪、摘要与状态契约通过"
