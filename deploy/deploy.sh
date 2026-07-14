#!/usr/bin/env bash
# 本地构建并上传部署包到服务器（需设置 SSHPASS 环境变量）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$ROOT/deploy"
DEPLOY_DOMAIN="${DEPLOY_DOMAIN:-https://rechargeai.cn}"
SERVER="${DEPLOY_SERVER:-root@8.218.19.217}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/wildai}"
JAR_NAME="wildai-backend-0.1.0-SNAPSHOT.jar"
HEALTH_ATTEMPTS="${DEPLOY_HEALTH_ATTEMPTS:-60}"
HEALTH_INTERVAL_SECONDS="${DEPLOY_HEALTH_INTERVAL_SECONDS:-2}"

: "${SSHPASS:?请先 export SSHPASS=你的服务器密码}"

if [[ ! "$HEALTH_ATTEMPTS" =~ ^[1-9][0-9]*$ || ! "$HEALTH_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "错误：健康检查次数和间隔必须是正整数" >&2
  exit 1
fi

sha256_file() {
  local file="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  else
    echo "错误：本机缺少 SHA256 计算工具" >&2
    return 1
  fi
}

remote_exec() {
  local remote_command="$1"
  SSHPASS="$SSHPASS" sshpass -e ssh -o StrictHostKeyChecking=no "$SERVER" \
    "export DOCKER_HOST=unix:///run/podman/podman.sock && cd '$REMOTE_DIR' && $remote_command"
}

wait_for_backend_health() {
  local attempt
  local health_status
  for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt++)); do
    health_status="$(remote_exec "docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' wildai-backend" 2>/dev/null || true)"
    if [[ "$health_status" == "healthy" ]]; then
      echo "==> 后端容器健康检查通过"
      return 0
    fi
    if ((attempt < HEALTH_ATTEMPTS)); then
      sleep "$HEALTH_INTERVAL_SECONDS"
    fi
  done
  echo "错误：后端容器未在限定时间内达到 healthy，最终状态为 ${health_status:-unknown}" >&2
  return 1
}

wait_for_public_ssr_ready() {
  local attempt
  local http_status
  local page_html
  local response
  local products_url="${DEPLOY_DOMAIN%/}/products"
  for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt++)); do
    response="$(curl -sS --max-time 10 --write-out $'\n%{http_code}' "$products_url" 2>/dev/null || true)"
    http_status="${response##*$'\n'}"
    page_html="${response%$'\n'*}"
    if [[ "$http_status" == "200" ]] && grep -q 'logo__mark' <<<"$page_html"; then
      echo "==> 公开 SSR 产品页就绪"
      return 0
    fi
    if ((attempt < HEALTH_ATTEMPTS)); then
      sleep "$HEALTH_INTERVAL_SECONDS"
    fi
  done
  echo "错误：公开 SSR 产品页未在限定时间内返回 200 且包含品牌标记" >&2
  return 1
}

verify_container_state() {
  local container
  local container_state
  local running
  local started_at
  for container in wildai-backend wildai-nginx; do
    container_state="$(remote_exec "docker inspect -f '{{.State.Running}}|{{.State.StartedAt}}' $container")"
    running="${container_state%%|*}"
    started_at="${container_state#*|}"
    if [[ "$running" != "true" || -z "$started_at" || "$started_at" == "$container_state" ]]; then
      echo "错误：容器 $container 状态核验失败" >&2
      return 1
    fi
    echo "    $container: Running=$running StartedAt=$started_at"
  done
}

export JAVA_HOME="${JAVA_HOME:-/Users/mjy/Library/Java/JavaVirtualMachines/openjdk-24.0.2+12-54/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> 构建后端..."
(cd "$ROOT/backend" && mvn package -DskipTests -q)

echo "==> 构建前端..."
(cd "$ROOT/frontend-user" && npm run build)
(cd "$ROOT/frontend-admin" && npm run build)

echo "==> 组装部署包..."
mkdir -p "$DEPLOY/html/user" "$DEPLOY/html/admin" \
  "$DEPLOY/uploads/tutorial" "$DEPLOY/uploads/article"
cp "$ROOT/backend/target/$JAR_NAME" "$DEPLOY/"
cp -r "$ROOT/frontend-user/dist/." "$DEPLOY/html/user/"
cp -r "$ROOT/frontend-admin/dist/." "$DEPLOY/html/admin/"

if [[ -f "$ROOT/ssl/fullchain.pem" && -f "$ROOT/ssl/privkey.key" ]]; then
  echo "==> 复制 SSL 证书..."
  mkdir -p "$DEPLOY/ssl"
  cp "$ROOT/ssl/fullchain.pem" "$ROOT/ssl/privkey.key" "$DEPLOY/ssl/"
elif [[ ! -f "$DEPLOY/ssl/fullchain.pem" ]]; then
  echo "错误: 未找到 SSL 证书，请将 fullchain.pem 与 privkey.key 放到项目 ssl/ 目录" >&2
  exit 1
fi

if [[ ! -f "$DEPLOY/.env" ]]; then
  echo "==> 生成 .env（首次部署）..."
  JWT=$(openssl rand -hex 24)
  ROOT_PW=$(openssl rand -hex 12)
  DB_PW=$(openssl rand -hex 12)
  cat > "$DEPLOY/.env" <<EOF
MYSQL_ROOT_PASSWORD=${ROOT_PW}
MYSQL_PASSWORD=${DB_PW}
JWT_SECRET=${JWT}
AES_SECRET_KEY=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=
WILDAI_PAYMENT_MOCK_ENABLED=false
XUNHUPAY_APPID=请填写虎皮椒APPID
XUNHUPAY_SECRET=请填写虎皮椒密钥
XUNHUPAY_GATEWAY=https://api.xunhupay.com
XUNHUPAY_NOTIFY_URL=https://rechargeai.cn/api/payments/xunhupay/notify
XUNHUPAY_RETURN_URL=https://rechargeai.cn/transaction-record
XUNHUPAY_CALLBACK_URL=https://rechargeai.cn/transaction-record
XUNHUPAY_TIMEOUT_SECONDS=10
WILDAI_SEO_BASE_URL=https://rechargeai.cn
WILDAI_ARTICLE_MAX_IMAGE_BYTES=5242880
EOF
fi

echo "==> 上传到 $SERVER:$REMOTE_DIR ..."
cd "$DEPLOY"
tar czf - --exclude='.env' . | SSHPASS="$SSHPASS" sshpass -e ssh -o StrictHostKeyChecking=no "$SERVER" "mkdir -p $REMOTE_DIR && cd $REMOTE_DIR && tar xzf -"

echo "==> 校验发布 JAR 摘要..."
LOCAL_JAR_SHA256="$(sha256_file "$DEPLOY/$JAR_NAME")"
REMOTE_JAR_SHA256="$(remote_exec "sha256sum $JAR_NAME" | awk '{print $1}')"
if [[ "$LOCAL_JAR_SHA256" != "$REMOTE_JAR_SHA256" ]]; then
  echo "错误：本地与远端 JAR SHA256 不一致，停止部署" >&2
  exit 1
fi
echo "    JAR SHA256: $LOCAL_JAR_SHA256"

echo "==> 强制重建后端（JAR 卷挂载，跳过镜像构建）..."
remote_exec "docker-compose -f docker-compose.prod.yml up -d --no-deps --no-build --force-recreate backend"
wait_for_backend_health

echo "==> 后端健康后强制重建 Nginx..."
remote_exec "docker-compose -f docker-compose.prod.yml up -d --no-deps --no-build --force-recreate nginx"
wait_for_public_ssr_ready
verify_container_state

echo "==> 部署完成"
echo "    用户端: ${DEPLOY_DOMAIN}/"
echo "    管理端: ${DEPLOY_DOMAIN}/admin/"
