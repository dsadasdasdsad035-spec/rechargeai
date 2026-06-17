# Quickstart: WildAI 订阅助手平台（MVP 一期）

**Feature**: `001-wildai-subscription-platform`  
**Goal**: 本地运行订单闭环（注册 → 浏览 → 下单 → 支付 → 交易记录 → 后台只读订单）

---

## 前置条件

- JDK 21+
- Node.js 20+ / pnpm 9+
- Docker & Docker Compose
- 微信支付/支付宝 **沙箱** 商户号（本地可先用 Mock 支付适配器）

---

## 1. 克隆与分支

```bash
git checkout 001-wildai-subscription-platform
```

---

## 2. 启动基础设施

```bash
docker compose up -d mysql redis
```

默认连接：
- MySQL: `localhost:3306/wildai`（用户 `wildai` / 密码见 `docker-compose.yml`）
- Redis: `localhost:6379`

---

## 3. 后端

```bash
cd backend
cp src/main/resources/application-local.example.yml application-local.yml
# 编辑支付沙箱密钥、JWT secret、加密密钥

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

健康检查：`GET http://localhost:8080/actuator/health`

Flyway 自动迁移：`backend/src/main/resources/db/migration/V1__phase1_schema.sql`

---

## 4. 用户端前端

```bash
cd frontend-user
pnpm install
pnpm dev
```

访问：`http://localhost:5173`  
交易记录页：`http://localhost:5173/transaction-record`

---

## 5. 管理端前端

```bash
cd frontend-admin
pnpm install
pnpm dev
```

访问：`http://localhost:5174`  
默认种子管理员：`admin` / `changeme`（首次启动由 Flyway seed 创建，**生产必须修改**）

---

## 6. 一期核心验证路径

### 6.1 用户下单支付

1. 注册/登录用户
2. 浏览产品列表，进入详情确认合规提示
3. 填写 AI 账号邮箱，提交订单
4. 选择微信/支付宝发起支付（沙箱或 Mock）
5. 模拟支付回调或完成沙箱支付
6. 打开 `/transaction-record` 确认订单状态为 **已支付**

### 6.2 重复订购拒绝

1. 对同一产品保留 `WAIT_PAY` 或 `PAID` 订单
2. 再次下单 → 期望 `409` 与提示文案

### 6.3 订单超时关闭

1. 创建订单不支付，等待 15 分钟（或调短 `order.expire-minutes` 配置做测试）
2. 确认订单状态变为 **已关闭**

### 6.4 后台只读

1. 管理员登录后台
2. 订单列表筛选、查看详情、导出 CSV
3. 确认 **无** 修改订单/支付状态按钮

---

## 7. 环境变量（关键）

| 变量 | 说明 |
|------|------|
| `WILDAI_JWT_SECRET` | JWT 签名密钥 |
| `WILDAI_AES_KEY` | AI 账号 AES 加密密钥（32 字节 Base64） |
| `WECHAT_MCH_ID` / `WECHAT_API_V3_KEY` | 微信商户（可 Mock） |
| `ALIPAY_APP_ID` / `ALIPAY_PRIVATE_KEY` | 支付宝（可 Mock） |
| `ORDER_EXPIRE_MINUTES` | 默认 `15` |

---

## 8. 测试

```bash
cd backend
./mvnw test

# 集成测试（需 Docker）
./mvnw verify -Pintegration
```

重点场景：支付回调幂等、重复订购、订单超时、脱敏展示。

---

## 9. 下一步

- `/speckit-tasks` — 生成可执行任务列表
- 二期启用前阅读 `data-model.md` Phase 2 预留表与 `spec.md` 澄清记录
