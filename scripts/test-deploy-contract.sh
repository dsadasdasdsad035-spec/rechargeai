#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT/deploy/deploy.sh"
COMPOSE_FILE="$ROOT/deploy/docker-compose.prod.yml"
NGINX_CONFIG="$ROOT/deploy/nginx.conf"

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
healthcheck_test="$(awk '
  /^      test:$/ { capture = 1 }
  capture && /^      (interval|timeout|retries|start_period):/ { exit }
  capture { print }
' <<<"$backend_block" \
  | sed -E '/^[[:space:]]*#/d; s/[[:space:]]+#.*$//' \
  | tr '\n' ' ')"
grep -q 'CMD-SHELL' <<<"$healthcheck_test" || fail "backend healthcheck test 必须执行 Shell 命令"
grep -qE 'wget .*\/actuator\/health.*status.*UP' <<<"$healthcheck_test" \
  || fail "backend healthcheck test 命令必须使用 wget 校验 actuator UP"

grep -qF "local remote_dir_pattern='^/[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*$'" "$DEPLOY_SCRIPT" \
  || fail "缺少安全远端绝对路径正则"
grep -qF 'if [[ ! "$REMOTE_DIR" =~ $remote_dir_pattern || "$REMOTE_DIR" =~ (^|/)\.{1,2}(/|$) ]]; then' "$DEPLOY_SCRIPT" \
  || fail "远端路径必须拒绝非法字符以及 . 或 .. 路径段"
grep -qF "local server_pattern='^([A-Za-z0-9]([A-Za-z0-9._-]*[A-Za-z0-9])?@)?[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$'" "$DEPLOY_SCRIPT" \
  || fail "缺少安全 SSH 服务器格式正则"
grep -qF 'if [[ ! "$SERVER" =~ $server_pattern' "$DEPLOY_SCRIPT" \
  || fail "部署脚本未校验 SSH 服务器输入"

input_validation_line="$(command_line '^[[:space:]]*validate_deploy_inputs[[:space:]]*$')"
first_ssh_line="$(command_line 'sshpass -e ssh')"
[[ -n "$input_validation_line" ]] || fail "缺少部署输入校验调用"
[[ -n "$first_ssh_line" ]] || fail "部署脚本缺少 SSH 调用"
if ! ((input_validation_line < first_ssh_line)); then
  fail "远端路径与服务器输入必须在首次 SSH 前完成校验"
fi

grep -qF '[[ ! "$HEALTH_ATTEMPTS" =~ ^[1-9][0-9]*$ || ! "$HEALTH_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]' "$DEPLOY_SCRIPT" \
  || fail "健康检查参数必须拒绝 0 和前导零"

actuator_block="$(awk '
  /^[[:space:]]*location \/actuator\/ \{/ { capture = 1 }
  capture { print }
  capture && /^[[:space:]]*\}$/ { exit }
' "$NGINX_CONFIG")"
[[ -n "$actuator_block" ]] || fail "Nginx 缺少受限 actuator 路由"
grep -qE '^[[:space:]]*allow 127\.0\.0\.1;' <<<"$actuator_block" \
  || fail "Nginx actuator 路由必须仅允许回环地址"
grep -qE '^[[:space:]]*deny all;' <<<"$actuator_block" \
  || fail "Nginx actuator 路由必须拒绝其他来源"

backend_restart_line="$(command_line '^[[:space:]]*remote_exec "docker-compose -f docker-compose\.prod\.yml up -d --no-deps --no-build --force-recreate backend"[[:space:]]*$')"
backend_wait_line="$(command_line '^[[:space:]]*wait_for_backend_health[[:space:]]*$')"
nginx_restart_line="$(command_line '^[[:space:]]*remote_exec "docker-compose -f docker-compose\.prod\.yml up -d --no-deps --no-build --force-recreate nginx"[[:space:]]*$')"
public_wait_line="$(command_line '^[[:space:]]*wait_for_public_ssr_ready[[:space:]]*$')"
state_verify_line="$(command_line '^[[:space:]]*verify_container_state[[:space:]]*$')"

[[ -n "$backend_restart_line" ]] || fail "缺少真实 backend 强制重建命令"
[[ -n "$backend_wait_line" ]] || fail "缺少 wait_for_backend_health 调用"
[[ -n "$nginx_restart_line" ]] || fail "缺少真实 nginx 强制重建命令"
[[ -n "$public_wait_line" ]] || fail "缺少 wait_for_public_ssr_ready 调用"
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
grep -qE '^wait_for_public_ssr_ready\(\)[[:space:]]*\{' "$DEPLOY_SCRIPT" \
  || fail "缺少 wait_for_public_ssr_ready 函数"
grep -qE '^[[:space:]]*local products_url=.*\/products' "$DEPLOY_SCRIPT" \
  || fail "公开 SSR 等待未定义产品页地址"
grep -qE '^[[:space:]]*response="\$\(curl .*--write-out .*http_code.*\$products_url' "$DEPLOY_SCRIPT" \
  || fail "公开 SSR 页面必须从部署机直接请求"
grep -qE '^[[:space:]]*if \[\[ "\$http_status" == "200" \]\] && grep -q .*logo__mark' "$DEPLOY_SCRIPT" \
  || fail "公开 SSR 页面必须校验 HTTP 200 与稳定品牌标记"
if grep -qE '^[[:space:]]*[^#].*curl .*actuator/health' "$DEPLOY_SCRIPT"; then
  fail "不得从外部路径请求受限 actuator 健康端点"
fi
grep -qE '^verify_container_state\(\)[[:space:]]*\{' "$DEPLOY_SCRIPT" \
  || fail "缺少 verify_container_state 函数"
grep -qE '^[[:space:]]*health_status=.*remote_exec ".*docker inspect -f .*State\.Health\.Status.*wildai-backend' "$DEPLOY_SCRIPT" \
  || fail "后端等待未读取容器 Health.Status"
grep -qE '^[[:space:]]*container_state=.*remote_exec ".*docker inspect -f .*State\.Running.*State\.StartedAt' "$DEPLOY_SCRIPT" \
  || fail "容器状态核验缺少 Running 或 StartedAt"

echo "部署脚本就绪、摘要、双层门禁与状态契约通过"
