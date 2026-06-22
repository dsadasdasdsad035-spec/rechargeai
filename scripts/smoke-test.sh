#!/usr/bin/env bash
# 线上冒烟测试：邮箱注册 + Mock 支付闭环
set -euo pipefail

BASE_URL="${SMOKE_BASE_URL:-http://rechargeai.cn}"
API="${BASE_URL}/api"
SSH_TARGET="${SMOKE_SSH:-root@8.218.19.217}"

echo "==> 冒烟测试目标: ${BASE_URL}"

EMAIL="smoke-$(date +%s)@example.com"
echo "==> 1. 发送邮箱验证码 -> ${EMAIL}"

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

echo "==> 2. 邮箱注册"
REG_RESP=$(curl -fsS -X POST "${API}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"type\":\"EMAIL\",\"email\":\"${EMAIL}\",\"verifyCode\":\"${CODE}\"}")

echo "${REG_RESP}" | grep -q '"code":"0"' || { echo "注册失败: ${REG_RESP}"; exit 1; }
TOKEN=$(echo "${REG_RESP}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
echo "    注册成功"

echo "==> 3. 创建订单"
ORDER_RESP=$(curl -fsS -X POST "${API}/orders" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"fields":{"target_account":"smoke@example.com"}}')

ORDER_NO=$(echo "${ORDER_RESP}" | sed -n 's/.*"orderNo":"\([^"]*\)".*/\1/p')
echo "    订单号: ${ORDER_NO}"

echo "==> 4. 发起 Mock 支付"
PAY_RESP=$(curl -fsS -X POST "${API}/orders/${ORDER_NO}/pay" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"channel":"WECHAT"}')

PAYMENT_NO=$(echo "${PAY_RESP}" | sed -n 's/.*"paymentNo":"\([^"]*\)".*/\1/p')
TRADE_NO=$(echo "${PAY_RESP}" | sed -n 's/.*"mockTradeNo":"\([^"]*\)".*/\1/p')
echo "    支付单: ${PAYMENT_NO}"

echo "==> 5. 模拟支付回调"
curl -fsS -X POST "${API}/payments/mock/notify?paymentNo=${PAYMENT_NO}&tradeNo=${TRADE_NO}" >/dev/null

echo "==> 6. 校验订单状态"
DETAIL=$(curl -fsS "${API}/orders/${ORDER_NO}" -H "Authorization: Bearer ${TOKEN}")
echo "${DETAIL}" | grep -q '"orderStatus":"PAID"' || { echo "订单未支付: ${DETAIL}"; exit 1; }
echo "${DETAIL}" | grep -q '"paymentStatus":"PAID"' || { echo "支付状态异常: ${DETAIL}"; exit 1; }

echo "==> 冒烟测试通过"
