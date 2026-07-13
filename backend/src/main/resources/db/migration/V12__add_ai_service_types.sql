-- 新增产品服务类型及默认教程配置

INSERT INTO service_type_config (service_type, display_name, account_tutorial, token_tutorial) VALUES
(
    'X_PREMIUM_PLUS',
    'X Premium+',
    '1. 打开 https://x.com 并登录需要开通 Premium+ 的账号\n2. 在个人资料或设置页确认用户名、邮箱\n3. 将该用户名或邮箱填写到「AI 账号」栏',
    '1. 在浏览器登录 X 后，按 F12 打开开发者工具\n2. 切到 Application → Cookies → x.com\n3. 复制 auth_token、ct0 等客服要求的完整会话凭证\n4. 粘贴到「Session Token」栏（请勿泄露给无关人员）'
),
(
    'GEMINI_PRO_ULTRA',
    'Gemini Pro / Ultra',
    '1. 打开 https://gemini.google.com 或 Google 账号页并登录\n2. 确认需要开通 Gemini Pro / Ultra 的 Google 邮箱\n3. 将该邮箱填写到「AI 账号」栏',
    '1. 如客服要求 API Key，请打开 Google AI Studio 并创建 API Key\n2. 如客服要求 Session Token，请按客服指引从浏览器 Cookie 中复制完整会话凭证\n3. 粘贴到「Session Token」栏（请勿填写登录密码）'
),
(
    'CLAUDE_API',
    'Claude API',
    '1. 打开 https://console.anthropic.com 并登录 Claude API 账号\n2. 确认控制台账号邮箱\n3. 将该邮箱填写到「AI 账号」栏',
    '1. 登录 Anthropic Console 后进入 API Keys 页面\n2. 创建或复制可用 API Key\n3. 粘贴到「Session Token」栏，并确认账号余额或套餐可用'
);
