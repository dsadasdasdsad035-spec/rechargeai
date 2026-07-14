#!/usr/bin/env bash
# 本地构建并上传部署包到服务器（需设置 SSHPASS 环境变量）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$ROOT/deploy"
DEPLOY_DOMAIN="${DEPLOY_DOMAIN:-https://rechargeai.cn}"
SERVER="${DEPLOY_SERVER:-root@8.218.19.217}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/wildai}"

: "${SSHPASS:?请先 export SSHPASS=你的服务器密码}"

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
cp "$ROOT/backend/target/wildai-backend-0.1.0-SNAPSHOT.jar" "$DEPLOY/"
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

echo "==> 重启服务（JAR 卷挂载，跳过镜像构建）..."
SSHPASS="$SSHPASS" sshpass -e ssh -o StrictHostKeyChecking=no "$SERVER" "export DOCKER_HOST=unix:///run/podman/podman.sock && cd $REMOTE_DIR && docker-compose -f docker-compose.prod.yml up -d --no-build backend nginx"

echo "==> 部署完成"
echo "    用户端: ${DEPLOY_DOMAIN}/"
echo "    管理端: ${DEPLOY_DOMAIN}/admin/"
