# Tasks: WildAI 订阅助手平台

**Input**: Design documents from `/specs/001-wildai-subscription-platform/`  
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅  
**Tests**: 规格未要求 TDD，本任务列表不含测试任务（集成验证见 Polish 阶段 quickstart）

**Organization**: 按一期用户故事分组（US1/US2/US5/US6/US7）；二期+ 故事（US3/US4/US8–US10）不在本列表实现

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: 用户故事标签（US1–US7 一期范围）

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 三工程骨架与本地开发环境

- [ ] T001 创建仓库根目录 `docker-compose.yml`（MySQL 8 + Redis 7 服务定义）
- [ ] T002 [P] 初始化 Spring Boot 工程 `backend/pom.xml`（Java 21、Spring Boot 3.3、JPA、Security、Flyway、Redis）
- [ ] T003 [P] 创建启动类 `backend/src/main/java/com/wildai/WildAiApplication.java`
- [ ] T004 [P] 初始化用户端 `frontend-user/`（Vite + Vue 3 + TypeScript + Pinia + Vue Router）
- [ ] T005 [P] 初始化管理端 `frontend-admin/`（Vite + Vue 3 + TypeScript + Element Plus + Vue Router）
- [ ] T006 [P] 添加后端配置模板 `backend/src/main/resources/application.yml` 与 `application-local.example.yml`
- [ ] T007 [P] 配置用户端 API 代理 `frontend-user/vite.config.ts`（`/api` → `localhost:8080`）
- [ ] T008 [P] 配置管理端 API 代理 `frontend-admin/vite.config.ts`（`/admin/api` → `localhost:8080`）

---

## Phase 2: Foundational（阻塞性前置）

**Purpose**: 所有用户故事依赖的公共能力 —— **完成前不得开始用户故事**

- [ ] T009 编写 Flyway 一期 schema `backend/src/main/resources/db/migration/V1__phase1_schema.sql`（user_account、ai_service_product、subscription_order、payment_transaction、admin_user）
- [ ] T010 [P] 实现统一响应体 `backend/src/main/java/com/wildai/common/dto/ApiResponse.java`
- [ ] T011 [P] 实现全局异常处理 `backend/src/main/java/com/wildai/common/exception/GlobalExceptionHandler.java`
- [ ] T012 [P] 实现业务错误码枚举 `backend/src/main/java/com/wildai/common/exception/ErrorCode.java`
- [ ] T013 [P] 实现脱敏工具 `backend/src/main/java/com/wildai/common/util/DesensitizeUtil.java`
- [ ] T014 [P] 实现 AES 加密工具 `backend/src/main/java/com/wildai/common/util/AesEncryptUtil.java`（AI 账号加密存储）
- [ ] T015 实现 JWT 工具与配置 `backend/src/main/java/com/wildai/common/security/JwtTokenProvider.java`
- [ ] T016 配置 Spring Security 过滤器链 `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`（区分 user/admin `aud`）
- [ ] T017 [P] 配置 Redis 连接 `backend/src/main/java/com/wildai/common/config/RedisConfig.java`
- [ ] T018 [P] 实现限流组件 `backend/src/main/java/com/wildai/common/ratelimit/RateLimitService.java`（登录/验证码/下单）
- [ ] T019 编写 admin 种子数据 `backend/src/main/resources/db/migration/V2__seed_admin.sql`
- [ ] T020 [P] 实现用户端 HTTP 客户端 `frontend-user/src/services/http.ts`（Bearer、401 刷新）
- [ ] T021 [P] 实现管理端 HTTP 客户端 `frontend-admin/src/services/http.ts`

**Checkpoint**: 基础架构就绪 —— 可开始用户故事实现

---

## Phase 3: User Story 5 - 用户注册与账户管理（Priority: P2）**（一期）**

**Goal**: 手机号/邮箱注册登录、JWT、资料管理、封禁拦截

**Independent Test**: 新用户注册登录后可访问受保护 API；DISABLED 用户创建订单返回 403

### Implementation

- [ ] T022 [P] [US5] 创建实体 `backend/src/main/java/com/wildai/auth/domain/UserAccount.java`
- [ ] T023 [P] [US5] 创建 Repository `backend/src/main/java/com/wildai/auth/repository/UserAccountRepository.java`
- [ ] T024 [US5] 实现验证码服务（Mock/Redis）`backend/src/main/java/com/wildai/auth/service/VerifyCodeService.java`
- [ ] T025 [US5] 实现 AuthService `backend/src/main/java/com/wildai/auth/service/AuthService.java`（注册、登录、刷新 Token）
- [ ] T026 [US5] 实现 AuthController `backend/src/main/java/com/wildai/auth/web/AuthController.java`（`/api/auth/*`）
- [ ] T027 [US5] 实现 UserService `backend/src/main/java/com/wildai/user/service/UserService.java`（资料更新、二次验证）
- [ ] T028 [US5] 实现 UserController `backend/src/main/java/com/wildai/user/web/UserController.java`（`/api/users/me`）
- [ ] T029 [P] [US5] 实现账户状态拦截 `backend/src/main/java/com/wildai/user/security/AccountStatusChecker.java`
- [ ] T030 [P] [US5] 创建 auth store `frontend-user/src/stores/auth.ts`
- [ ] T031 [P] [US5] 创建登录/注册页 `frontend-user/src/pages/LoginPage.vue` 与 `RegisterPage.vue`
- [ ] T032 [US5] 配置用户端路由守卫 `frontend-user/src/router/index.ts`

**Checkpoint**: US5 可独立验证注册登录流程

---

## Phase 4: User Story 6 - 后台产品管理（Priority: P3）**（一期）**

**Goal**: 管理员配置产品；用户端浏览上架产品详情与合规提示

**Independent Test**: 后台上架产品后用户端可见；下架后用户端不可下单

### Implementation

- [ ] T033 [P] [US6] 创建实体 `backend/src/main/java/com/wildai/product/domain/AiServiceProduct.java`
- [ ] T034 [P] [US6] 创建 Repository `backend/src/main/java/com/wildai/product/repository/AiServiceProductRepository.java`
- [ ] T035 [US6] 实现 ProductService `backend/src/main/java/com/wildai/product/service/ProductService.java`（上下架、required_fields 校验）
- [ ] T036 [US6] 实现用户端 ProductController `backend/src/main/java/com/wildai/product/web/ProductController.java`（`GET /api/products`）
- [ ] T037 [US6] 实现管理端 AdminProductController `backend/src/main/java/com/wildai/admin/web/AdminProductController.java`（CRUD + shelf）
- [ ] T038 [P] [US6] 创建产品 DTO `backend/src/main/java/com/wildai/product/dto/ProductDetailDto.java`
- [ ] T039 [P] [US6] 创建产品 API `frontend-user/src/services/productApi.ts`
- [ ] T040 [P] [US6] 创建产品列表页 `frontend-user/src/pages/ProductListPage.vue`
- [ ] T041 [US6] 创建产品详情页 `frontend-user/src/pages/ProductDetailPage.vue`（规则、退款说明、合规提示）
- [ ] T042 [P] [US6] 创建管理端产品 API `frontend-admin/src/services/productApi.ts`
- [ ] T043 [US6] 创建管理端产品视图 `frontend-admin/src/views/ProductListView.vue` 与 `ProductFormView.vue`

**Checkpoint**: US6 可独立验证产品上下架与用户端展示

---

## Phase 5: User Story 1 - 浏览并订购 AI 订阅服务（Priority: P1）**（一期）** 🎯 MVP 核心

**Goal**: 登录用户选产品、填 AI 账号、下单、微信/支付宝支付，支付成功后订单 `PAID`

**Independent Test**: 完成支付后交易记录可见「已支付」订单；重复订购返回 409；15 分钟未支付自动 CLOSED

### Implementation

- [ ] T044 [P] [US1] 创建订单实体 `backend/src/main/java/com/wildai/order/domain/SubscriptionOrder.java`
- [ ] T045 [P] [US1] 创建支付流水实体 `backend/src/main/java/com/wildai/payment/domain/PaymentTransaction.java`
- [ ] T046 [P] [US1] 创建 OrderRepository `backend/src/main/java/com/wildai/order/repository/SubscriptionOrderRepository.java`
- [ ] T047 [P] [US1] 创建 PaymentRepository `backend/src/main/java/com/wildai/payment/repository/PaymentTransactionRepository.java`
- [ ] T048 [US1] 实现重复订购校验 `backend/src/main/java/com/wildai/order/service/DuplicateOrderChecker.java`
- [ ] T049 [US1] 实现 OrderService `backend/src/main/java/com/wildai/order/service/OrderService.java`（创建、状态流转、expired_at）
- [ ] T050 [US1] 定义支付渠道接口 `backend/src/main/java/com/wildai/payment/channel/PaymentChannelAdapter.java`
- [ ] T051 [P] [US1] 实现 Mock 支付适配器 `backend/src/main/java/com/wildai/payment/channel/MockPaymentAdapter.java`
- [ ] T052 [P] [US1] 实现微信支付适配器 `backend/src/main/java/com/wildai/payment/channel/WechatPaymentAdapter.java`
- [ ] T053 [P] [US1] 实现支付宝适配器 `backend/src/main/java/com/wildai/payment/channel/AlipayPaymentAdapter.java`
- [ ] T054 [US1] 实现 PaymentService `backend/src/main/java/com/wildai/payment/service/PaymentService.java`（创建支付单、回调幂等）
- [ ] T055 [US1] 实现 OrderController `backend/src/main/java/com/wildai/order/web/OrderController.java`（`POST /api/orders`）
- [ ] T056 [US1] 实现 PayController `backend/src/main/java/com/wildai/order/web/OrderPayController.java`（`POST /api/orders/{orderNo}/pay`）
- [ ] T057 [US1] 实现支付回调 `backend/src/main/java/com/wildai/payment/web/PaymentCallbackController.java`（微信/支付宝 notify）
- [ ] T058 [US1] 实现订单超时关闭定时任务 `backend/src/main/java/com/wildai/order/job/OrderExpireJob.java`
- [ ] T059 [P] [US1] 创建订单 API `frontend-user/src/services/orderApi.ts`
- [ ] T060 [US1] 创建下单页 `frontend-user/src/pages/CheckoutPage.vue`（动态 required_fields、禁止密码字段）
- [ ] T061 [US1] 创建支付页 `frontend-user/src/pages/PaymentPage.vue`（二维码/支付参数展示）

**Checkpoint**: US1 可独立验证完整下单支付闭环

---

## Phase 6: User Story 2 - 查看交易记录与订单详情（Priority: P1）**（一期）**

**Goal**: `/transaction-record` 列表筛选、详情脱敏、空状态引导

**Independent Test**: 用户只看自己的订单；敏感字段脱敏；一期详情无履约日志/退款入口

### Implementation

- [ ] T062 [US2] 扩展 OrderService 列表查询 `backend/src/main/java/com/wildai/order/service/OrderQueryService.java`（分页、多条件筛选）
- [ ] T063 [US2] 扩展 OrderController `backend/src/main/java/com/wildai/order/web/OrderController.java`（`GET /api/orders`、`GET /api/orders/{orderNo}`）
- [ ] T064 [US2] 实现订单详情 DTO 脱敏组装 `backend/src/main/java/com/wildai/order/dto/OrderDetailDto.java`
- [ ] T065 [P] [US2] 创建交易记录页 `frontend-user/src/pages/TransactionRecordPage.vue`（路由 `/transaction-record`）
- [ ] T066 [P] [US2] 创建订单详情组件 `frontend-user/src/components/OrderDetailDrawer.vue`
- [ ] T067 [US2] 实现列表筛选与空状态 `frontend-user/src/pages/TransactionRecordPage.vue`（状态/时间/服务类型）
- [ ] T068 [P] [US2] 移动端卡片布局样式 `frontend-user/src/pages/TransactionRecordPage.vue`

**Checkpoint**: US2 可独立验证交易记录与详情

---

## Phase 7: User Story 7 - 后台订单只读查看（Priority: P3）**（一期）**

**Goal**: 后台订单列表、详情、CSV 导出；**无**状态变更入口

**Independent Test**: 管理员可筛选导出；界面与 API 均不提供改状态操作

### Implementation

- [ ] T069 [US7] 实现 AdminAuthController `backend/src/main/java/com/wildai/admin/web/AdminAuthController.java`（`/admin/api/auth/login`）
- [ ] T070 [US7] 实现 AdminOrderQueryService `backend/src/main/java/com/wildai/admin/service/AdminOrderQueryService.java`
- [ ] T071 [US7] 实现 AdminOrderController `backend/src/main/java/com/wildai/admin/web/AdminOrderController.java`（list/detail/export 只读）
- [ ] T072 [US7] 实现 CSV 导出 `backend/src/main/java/com/wildai/admin/service/OrderExportService.java`
- [ ] T073 [P] [US7] 创建管理端订单 API `frontend-admin/src/services/orderApi.ts`
- [ ] T074 [US7] 创建订单只读列表 `frontend-admin/src/views/OrderListView.vue`（无编辑/改状态按钮）
- [ ] T075 [US7] 创建订单详情只读页 `frontend-admin/src/views/OrderDetailView.vue`
- [ ] T076 [US7] 实现管理端用户管理 `backend/src/main/java/com/wildai/admin/web/AdminUserController.java` + `frontend-admin/src/views/UserListView.vue`（封禁/解封，支撑 US5 验收）

**Checkpoint**: US7 + 用户封禁构成一期后台闭环

---

## Phase 8: Polish & Cross-Cutting（横切收尾）

**Purpose**: 非功能需求、文档与一期验收

- [ ] T077 [P] 补充 Actuator 健康检查 `backend/src/main/resources/application.yml`（`/actuator/health`）
- [ ] T078 实现支付补偿查单任务（可选）`backend/src/main/java/com/wildai/payment/job/PaymentReconcileJob.java`
- [ ] T079 [P] 补充 README `README.md`（指向 quickstart.md）
- [ ] T080 按 `specs/001-wildai-subscription-platform/quickstart.md` 执行一期验收并记录结果
- [ ] T081 [P] 更新 `.cursor/rules/specify-rules.mdc` 构建命令为实际可运行状态

---

## Deferred: 二期+ 用户故事（不在本 tasks 范围）

| 故事 | 标题 | 计划阶段 |
|------|------|----------|
| US3 | 履约进度跟踪 | 二期 |
| US4 | 申请退款 | 二期 |
| US8 | 后台履约任务处理 | 二期 |
| US9 | 后台退款审核 | 二期 |
| US10 | 后台权限与审计 | 二期 |

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (Phase 1)
  → Foundational (Phase 2) [BLOCKS ALL]
    → US5 (Phase 3) [auth 前置]
      → US6 (Phase 4) [产品]
        → US1 (Phase 5) [下单支付，依赖 US5+US6]
          → US2 (Phase 6) [交易记录，依赖 US1 订单数据]
            → US7 (Phase 7) [后台，依赖 US1 订单]
              → Polish (Phase 8)
```

### User Story Dependencies

| 故事 | 依赖 | 说明 |
|------|------|------|
| US5 | Foundational | 认证基础，US1/US2 前置 |
| US6 | US5（管理员登录） | 用户端产品浏览可仅依赖 Foundational+seed 产品 |
| US1 | US5 + US6 | 需登录 + 上架产品 |
| US2 | US1 | 需有订单数据 |
| US7 | US1 | 后台查看已产生订单 |

### Parallel Opportunities

**Setup 阶段可并行**: T002–T008  
**Foundational 可并行**: T010–T014、T017–T018、T020–T021  
**US1 支付适配器可并行**: T051–T053  
**US2 前端可并行**: T065–T066、T068  
**跨故事并行（Foundational 完成后）**: US5 与 US6 后端可由不同开发者并行（注意 T019 admin seed）

### Parallel Example: US1

```bash
# 并行创建实体与 Repository
T044: backend/.../order/domain/SubscriptionOrder.java
T045: backend/.../payment/domain/PaymentTransaction.java
T046: backend/.../order/repository/SubscriptionOrderRepository.java
T047: backend/.../payment/repository/PaymentTransactionRepository.java

# 并行实现支付渠道适配器
T051: MockPaymentAdapter.java
T052: WechatPaymentAdapter.java
T053: AlipayPaymentAdapter.java
```

---

## Implementation Strategy

### MVP First（最小可演示）

1. Phase 1–2: Setup + Foundational  
2. Phase 3: US5（能登录）  
3. Phase 4: US6（有产品）  
4. Phase 5: US1（能下单支付）→ **STOP 可演示 MVP**  
5. Phase 6–7: US2 + US7 → 完整一期  
6. Phase 8: Polish + quickstart 验收

### 一期交付标准（SC-010）

用户可完成：**注册 → 浏览 → 下单支付 → 查看交易记录 → 后台订单只读管理**

### 任务统计

| 阶段 | 任务数 |
|------|--------|
| Setup | 8 |
| Foundational | 13 |
| US5 | 11 |
| US6 | 11 |
| US1 | 18 |
| US2 | 7 |
| US7 | 8 |
| Polish | 5 |
| **合计** | **81** |

---

## Notes

- 所有任务描述含目标文件路径，便于 LLM 直接执行
- 一期支付成功后 **不** 创建履约任务（`fulfillment_status = NOT_STARTED`）
- 重复订购一律拒绝（FR-011）
- 后台订单一期 **只读**（FR-033a）
- 建议每完成一个 Checkpoint 提交一次 Git

**Suggested next command**: `/speckit-implement`
