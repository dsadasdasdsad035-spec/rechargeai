# Quickstart: RechargeAi 订阅助手平台（MVP 一期）

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

# 冒烟 / 集成（需 Docker）
./mvnw test -Dtest=Phase1SmokeIT,Phase2FlowIT,Phase3SmokeIT,LedgerConcurrencyTest
```

重点场景：支付回调幂等、重复订购、订单超时、脱敏展示。

---

## 9. 后续迭代（设计文档）

| 阶段 | 文档 | 说明 |
|------|------|------|
| 二期 | [spec.md](./spec.md) US 3–4、8–10 | 履约、退款、通知、RBAC；Flyway V2 |
| 三期 | [spec.md](./spec.md) US 11–13、[api-admin-phase3.openapi.yaml](./contracts/api-admin-phase3.openapi.yaml) | 账本、`SUCCESS` 结算、提现；Flyway V3 |

关键口径（已澄清）：
- 订单履约成功 → `order_status = SUCCESS`
- 可提现/运营收入仅统计 `SUCCESS` 订单
- 提现收款账户必选 `PayoutAccount` 预设

---

## 11. 三期资金与提现验证（US11–13）

**前置**：Flyway V4 已迁移；至少有一笔 `order_status = SUCCESS` 订单（履约成功）以产生可提现余额。

1. **超管**登录管理端 → **收款账户**（`/finance/payout-accounts`）新增启用账户
2. **财务**角色登录 → **资金管理** → **提现管理**（`/finance/withdrawals`）
   - 发起提现（≥ ¥100，选择启用账户）→ 状态 `PENDING_APPROVAL`，账本出现 `WITHDRAW_FREEZE`
3. **超管**审批通过 → `APPROVED`；尝试审批自己发起的单应被拒绝
4. 驳回场景：超管驳回 → `REJECTED`，账本 `WITHDRAW_RELEASE`，可提现余额恢复
5. **财务**对 `APPROVED` 单 **确认打款**（填写凭证号）→ `COMPLETED`，账本 `WITHDRAW_COMPLETE`
6. **资金概览**（`/finance`）核对：已结算实收 = SUCCESS 订单合计；可提现余额与账本末笔 `balance_after` 一致
7. 导出：`GET /admin/api/withdrawals/export` 或页面「导出 CSV」

配置项（`application.yml` → `wildai.finance`）：最低提现 ¥100、日上限 ¥50,000、待处理上限 3 笔、打款超时 7 天。

---

## 12. 二期/三期自动化验收记录（T144）

**执行日期**：2026-06-24

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn test -Dtest=Phase1SmokeIT,Phase2FlowIT,Phase3SmokeIT,LedgerConcurrencyTest
```

| 用例 | 覆盖范围 | 结果 |
|------|----------|------|
| `Phase1SmokeIT` | 一期：注册/登录/支付/履约/RBAC（9 用例） | ✓ PASS |
| `Phase2FlowIT` | 二期：履约失败→退款审核 | ✓ PASS |
| `Phase3SmokeIT` | 三期：SUCCESS 结算、提现申请/审批/打款/驳回（4 用例） | ✓ PASS |
| `LedgerConcurrencyTest` | SC-013：并发提现不超额冻结 | ✓ PASS |

---

## 10. 下一步

- `/speckit-tasks` — 生成可执行任务列表（一期）
- 二期/三期实现前阅读 [plan.md](./plan.md) 分阶段 Implementation Order
