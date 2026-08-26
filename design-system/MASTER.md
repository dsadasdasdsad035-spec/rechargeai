# RechargeAi Design System (ui-ux-pro-max)

**Product**: 订阅助手 / 支付服务 SaaS  
**Style**: Soft UI Evolution + Trust & Authority  
**Pattern**: Conversion-Optimized（支付信任感、清晰 CTA）

## Typography

| Role | Font | Weight |
|------|------|--------|
| Heading / UI | Plus Jakarta Sans | 600–700 |
| Body (中文) | Noto Sans SC | 400–500 |
| Price / Data | Plus Jakarta Sans | 600, tabular-nums |

## Color Palette (SaaS Trust Blue)

| Token | Hex | Usage |
|-------|-----|-------|
| Primary | `#2563EB` | 品牌、链接、主按钮 |
| Primary Hover | `#1D4ED8` | 悬停 |
| Accent (CTA) | `#EA580C` | 支付、提交订单 |
| Background | `#F8FAFC` | 页面底 |
| Surface | `#FFFFFF` | 卡片、导航 |
| Text Heading | `#0F172A` | 标题 |
| Text Body | `#334155` | 正文 |
| Text Muted | `#64748B` | 辅助 |
| Border | `#E2E8F0` | 分割线 |
| Success | `#059669` | 成功状态 |
| Danger | `#DC2626` | 错误 |

## Effects (Soft UI Evolution)

- Card radius: 12px (`--radius-md`)
- Shadow: 多层柔和阴影，hover 轻微上浮 2px
- Header: 毛玻璃 `backdrop-filter: blur(16px)`
- Motion: 200ms ease-out；尊重 `prefers-reduced-motion`
- Touch: 最小点击区域 44×44px

## Anti-Patterns

- 不用 emoji 作图标
- 不用纯 hover 交互（移动端可点）
- 不用低于 4.5:1 的文本对比度
