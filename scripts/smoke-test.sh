#!/usr/bin/env bash
# 线上冒烟测试：管理端新建产品 + 邮箱注册 + Mock 支付闭环
set -euo pipefail

BASE_URL="${SMOKE_BASE_URL:-https://rechargeai.cn}"
API="${BASE_URL}/api"
ADMIN_API="${BASE_URL}/admin/api"
SSH_TARGET="${SMOKE_SSH:-root@8.218.19.217}"
ADMIN_USER="${SMOKE_ADMIN_USER:-admin}"
ADMIN_PASS="${SMOKE_ADMIN_PASS:-changeme}"

echo "==> 冒烟测试目标: ${BASE_URL}"

echo "==> 1. 管理端登录"
ADMIN_RESP=$(curl -fsS -X POST "${ADMIN_API}/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")

echo "${ADMIN_RESP}" | grep -q '"code":"0"' || { echo "管理端登录失败: ${ADMIN_RESP}"; exit 1; }
ADMIN_TOKEN=$(echo "${ADMIN_RESP}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
echo "    登录成功"

PRODUCT_NAME="冒烟产品-$(date +%s)"
echo "==> 2. 新建产品 -> ${PRODUCT_NAME}"
PRODUCT_RESP=$(curl -fsS -X POST "${ADMIN_API}/products" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"${PRODUCT_NAME}\",\"salePrice\":1.00,\"periodDays\":30,\"serviceType\":\"GENERAL\"}")

echo "${PRODUCT_RESP}" | grep -q '"code":"0"' || { echo "新建产品失败: ${PRODUCT_RESP}"; exit 1; }
PRODUCT_ID=$(echo "${PRODUCT_RESP}" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "    产品 ID: ${PRODUCT_ID}"

echo "==> 3. 产品上架"
SHELF_RESP=$(curl -fsS -X POST "${ADMIN_API}/products/${PRODUCT_ID}/shelf" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"status":"ON_SHELF"}')
echo "${SHELF_RESP}" | grep -q '"code":"0"' || { echo "上架失败: ${SHELF_RESP}"; exit 1; }
echo "    已上架"

EMAIL="smoke-$(date +%s)@example.com"
echo "==> 4. 发送邮箱验证码 -> ${EMAIL}"

SEND_RESP=$(curl -fsS -X POST "${API}/auth/send-code" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\"}")

echo "${SEND_RESP}" | grep -q '"code":"0"' || { echo "发送验证码失败: ${SEND_RESP}"; exit 1; }

CODE=""
if echo "${SEND_RESP}" | grep -q devCode; then
  CODE=$(echo "${SEND_RESP}" | sed -n 's/.*"devCode":"\([^"]*\)".*/\1/p')
  echo "    使用开发模式 devCode"
elif [ -n "${SMOKE_VERIFY_CODE:-}" ]; then
  CODE="${SMOKE_VERIFY_CODE}"
  echo "    使用环境变量 SMOKE_VERIFY_CODE"
elif [ -n "${SSHPASS:-}" ]; then
  echo "    从服务器 Redis 读取验证码..."
  sleep 1
  CODE=$(SSHPASS="${SSHPASS}" sshpass -e ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o PubkeyAuthentication=no "${SSH_TARGET}" \
    "export DOCKER_HOST=unix:///run/podman/podman.sock && docker exec wildai-redis redis-cli GET 'vc:${EMAIL}'" | tr -d '\r')
fi

if [ -z "${CODE}" ]; then
  echo "无法获取验证码：请查收邮件，或设置 SMOKE_VERIFY_CODE / SSHPASS 后重试"
  exit 1
fi

echo "==> 5. 邮箱注册"
REG_RESP=$(curl -fsS -X POST "${API}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"type\":\"EMAIL\",\"email\":\"${EMAIL}\",\"verifyCode\":\"${CODE}\"}")

echo "${REG_RESP}" | grep -q '"code":"0"' || { echo "注册失败: ${REG_RESP}"; exit 1; }
TOKEN=$(echo "${REG_RESP}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
echo "    注册成功"

echo "==> 6. 创建订单（产品 ${PRODUCT_ID}）"
ORDER_RESP=$(curl -fsS -X POST "${API}/orders" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":${PRODUCT_ID},\"fields\":{\"target_account\":\"smoke@example.com\"}}")

ORDER_NO=$(echo "${ORDER_RESP}" | sed -n 's/.*"orderNo":"\([^"]*\)".*/\1/p')
echo "    订单号: ${ORDER_NO}"

echo "==> 7. 发起虎皮椒支付"
PAY_RESP=$(curl -fsS -X POST "${API}/orders/${ORDER_NO}/pay" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"channel":"XUNHUPAY"}')

echo "${PAY_RESP}" | grep -q '"code":"0"' || { echo "发起支付失败: ${PAY_RESP}"; exit 1; }

if echo "${PAY_RESP}" | grep -q mockTradeNo; then
  PAYMENT_NO=$(echo "${PAY_RESP}" | sed -n 's/.*"paymentNo":"\([^"]*\)".*/\1/p')
  TRADE_NO=$(echo "${PAY_RESP}" | sed -n 's/.*"mockTradeNo":"\([^"]*\)".*/\1/p')
  echo "    支付单: ${PAYMENT_NO}（Mock）"
  echo "==> 8. 模拟支付回调"
  curl -fsS -X POST "${API}/payments/mock/notify?paymentNo=${PAYMENT_NO}&tradeNo=${TRADE_NO}" >/dev/null
  echo "==> 9. 校验订单状态"
  DETAIL=$(curl -fsS "${API}/orders/${ORDER_NO}" -H "Authorization: Bearer ${TOKEN}")
  echo "${DETAIL}" | grep -q '"orderStatus":"PAID"' || { echo "订单未支付: ${DETAIL}"; exit 1; }
  echo "${DETAIL}" | grep -q '"paymentStatus":"PAID"' || { echo "支付状态异常: ${DETAIL}"; exit 1; }
else
  PAYMENT_URL=$(echo "${PAY_RESP}" | sed -n 's/.*"paymentUrl":"\([^"]*\)".*/\1/p')
  echo "    真实支付已创建"
  [ -n "${PAYMENT_URL}" ] && echo "    支付链接: ${PAYMENT_URL}"
  echo "==> 8-9. 跳过自动支付（真实支付需扫码完成）"
fi

echo "==> 冒烟测试通过"
