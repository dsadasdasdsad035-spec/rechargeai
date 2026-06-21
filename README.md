# WildAI 订阅助手平台

MVP 一期：用户注册登录、产品浏览、下单支付、交易记录、后台产品/用户/订单只读管理。

## 快速启动

```bash
# 1. 基础设施
docker compose up -d

# 2. 后端
cd backend && mvn spring-boot:run

# 3. 用户端
cd frontend-user && npm run dev

# 4. 管理端
cd frontend-admin && npm run dev
```

- 用户端：http://localhost:5173
- 管理端：http://localhost:5174（admin / changeme）
- 交易记录：http://localhost:5173/transaction-record

详细验收步骤见 `specs/001-wildai-subscription-platform/quickstart.md`。

## 虎皮椒支付

本地默认启用 mock 支付。启用真实虎皮椒支付时，将以下环境变量配置到后端运行环境，并把 `WILDAI_PAYMENT_MOCK_ENABLED` 设为 `false`：

```bash
XUNHUPAY_APPID=你的虎皮椒APPID
XUNHUPAY_SECRET=你的虎皮椒密钥
XUNHUPAY_GATEWAY=https://api.xunhupay.com
XUNHUPAY_NOTIFY_URL=https://你的域名/api/payments/xunhupay/notify
XUNHUPAY_RETURN_URL=https://你的域名/user/transaction-record
XUNHUPAY_CALLBACK_URL=https://你的域名/user/transaction-record
```

`XUNHUPAY_NOTIFY_URL` 必须是公网可访问地址，虎皮椒支付成功后会回调 `/api/payments/xunhupay/notify`。

## 技术栈

- 后端：Java 21 + Spring Boot 3.3 + JPA + Flyway + MySQL + Redis
- 用户端：Vue 3 + Vite + Pinia
- 管理端：Vue 3 + Element Plus

## 一期边界

- 支付成功后订单状态为 `PAID`，不创建履约任务
- 后台订单只读（列表/详情/导出）
- 重复订购一律拒绝
