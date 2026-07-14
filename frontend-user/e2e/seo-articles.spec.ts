import {
  expect,
  test,
  type APIRequestContext,
  type Page,
  type TestInfo,
} from '@playwright/test'

function assertWriteTargetIsAllowed(testInfo: TestInfo) {
  const baseURL = testInfo.project.use.baseURL
  if (!baseURL) {
    throw new Error('写入型 E2E 缺少 baseURL，已拒绝执行。')
  }

  const hostname = new URL(baseURL).hostname
  const isLocalTarget = hostname === 'localhost' || hostname === '127.0.0.1'
  if (!isLocalTarget && process.env.E2E_ALLOW_REMOTE_WRITES !== 'true') {
    throw new Error(
      `写入型 E2E 已拒绝访问远程主机 ${hostname}；如确认是隔离测试环境，请显式设置 E2E_ALLOW_REMOTE_WRITES=true。`,
    )
  }
}

function expectConfiguredBrowser(projectName: string, browserName: string | undefined) {
  const expectedBrowserName = projectName === 'firefox'
    ? 'firefox'
    : projectName === 'webkit'
      ? 'webkit'
      : 'chromium'
  expect(browserName).toBe(expectedBrowserName)
}

async function expectNoHorizontalOverflow(page: Page) {
  const hasNoHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
  )
  expect(hasNoHorizontalOverflow).toBeTruthy()
}

async function expectResponsiveNavigation(page: Page, projectName: string) {
  if (projectName === 'mobile-chromium') {
    const menuToggle = page.locator('.menu-toggle')
    await expect(menuToggle).toBeVisible()
    await menuToggle.click()

    const mobilePanel = page.locator('.mobile-nav__panel')
    await expect(mobilePanel).toBeVisible()
    for (const linkName of ['服务', '文章', '交易记录', '登录']) {
      await expect(mobilePanel.getByRole('link', { name: linkName, exact: true })).toBeVisible()
    }
    await expectNoHorizontalOverflow(page)
    return
  }

  await expect(page.locator('.nav-links')).toBeVisible()
}

async function cleanupArticle(
  request: APIRequestContext,
  articleId: number,
  authorization: { Authorization: string },
  needsWithdraw: boolean,
) {
  const cleanupErrors: string[] = []

  if (needsWithdraw) {
    try {
      const withdrawResponse = await request.post(`/admin/api/articles/${articleId}/withdraw`, {
        headers: authorization,
      })
      if (!withdrawResponse.ok()) {
        cleanupErrors.push(`撤回文章失败：HTTP ${withdrawResponse.status()}`)
      }
    } catch (error) {
      cleanupErrors.push(`撤回文章异常：${error instanceof Error ? error.message : String(error)}`)
    }
  }

  try {
    const deleteResponse = await request.delete(`/admin/api/articles/${articleId}`, {
      headers: authorization,
    })
    if (!deleteResponse.ok()) {
      cleanupErrors.push(`删除文章失败：HTTP ${deleteResponse.status()}`)
    }
  } catch (error) {
    cleanupErrors.push(`删除文章异常：${error instanceof Error ? error.message : String(error)}`)
  }

  if (cleanupErrors.length > 0) {
    throw new Error(`文章清理失败：\n- ${cleanupErrors.join('\n- ')}`)
  }
}

test('文章发布后可抓取，撤回后返回真实 404', async ({ page, request, browserName }, testInfo) => {
  assertWriteTargetIsAllowed(testInfo)
  expectConfiguredBrowser(testInfo.project.name, browserName)

  const username = process.env.E2E_ADMIN_USER ?? 'admin'
  const password = process.env.E2E_ADMIN_PASS ?? 'changeme'
  const unique = `${testInfo.project.name}-${Date.now()}`
  const slug = `seo-${unique}`
  const title = `跨浏览器 SEO 验收 ${unique}`
  const longUnbrokenText = `LONG_UNBROKEN_${'x'.repeat(320)}`
  const longCodeLine = `const longToken = '${'y'.repeat(260)}'`
  const wideTableCell = `宽表格_${'列'.repeat(160)}`

  const loginResponse = await request.post('/admin/api/auth/login', {
    data: { username, password },
  })
  expect(loginResponse.ok()).toBeTruthy()
  const loginBody = await loginResponse.json()
  expect(loginBody.code).toBe('0')
  const authorization = { Authorization: `Bearer ${loginBody.data.accessToken}` }

  let articleId: number | undefined
  let articlePublished = false
  let articleWithdrawn = false
  try {
    const createResponse = await request.post('/admin/api/articles', {
      headers: authorization,
      data: {
        title,
        slug,
        summary: '验证服务端渲染、规范链接与结构化数据。',
        contentMarkdown: [
          `# ${title}`,
          '',
          '这是无需执行 JavaScript 即可读取的正文。',
          '',
          longUnbrokenText,
          '',
          '```ts',
          longCodeLine,
          '```',
          '',
          '| 项目 | 内容 |',
          '| --- | --- |',
          `| 宽表格 | ${wideTableCell} |`,
        ].join('\n'),
        seoTitle: '',
        seoDescription: '',
      },
    })
    expect(createResponse.ok()).toBeTruthy()
    const created = await createResponse.json()
    expect(created.code).toBe('0')
    articleId = created.data.id

    const publishResponse = await request.post(`/admin/api/articles/${articleId}/publish`, {
      headers: authorization,
    })
    expect(publishResponse.ok()).toBeTruthy()
    articlePublished = true

    const articleListResponse = await page.goto('/articles')
    expect(articleListResponse?.status()).toBe(200)
    await expectNoHorizontalOverflow(page)
    await expectResponsiveNavigation(page, testInfo.project.name)
    await expect(page.locator('.article-card').filter({ hasText: title })).toBeVisible()

    const articleResponse = await page.goto(`/articles/${slug}`)
    expect(articleResponse?.status()).toBe(200)
    await expectNoHorizontalOverflow(page)
    await expectResponsiveNavigation(page, testInfo.project.name)
    await expect(page.locator('.logo__mark')).toHaveText('R')
    const logoFontFamily = await page.locator('.logo__text').evaluate(
      (element) => getComputedStyle(element).fontFamily,
    )
    expect(logoFontFamily).toContain('Georgia')
    const articleHeadingFontSize = await page.locator('.article-heading h1').evaluate(
      (element) => Number.parseFloat(getComputedStyle(element).fontSize),
    )
    expect(articleHeadingFontSize).toBeLessThanOrEqual(36)
    await expect(page.getByRole('heading', { name: title, exact: true }).first()).toBeVisible()
    await expect(page.getByText('这是无需执行 JavaScript 即可读取的正文。')).toBeVisible()
    await expect(page.getByText(longUnbrokenText, { exact: true })).toBeVisible()
    await expect(page.locator('.article-body pre').filter({ hasText: longCodeLine })).toBeVisible()
    await expect(page.locator('.article-body table').filter({ hasText: wideTableCell })).toBeVisible()
    await expectNoHorizontalOverflow(page)
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
      'href',
      `https://rechargeai.cn/articles/${slug}`,
    )
    await expect(page.locator('script[type="application/ld+json"]')).toHaveCount(1)
    await expect(page.locator("a.nav__link[href='/articles']").first()).toHaveText('文章')

    const productsResponse = await page.goto('/products')
    expect(productsResponse?.status()).toBe(200)
    await expectNoHorizontalOverflow(page)
    await expectResponsiveNavigation(page, testInfo.project.name)
    await expect(page.getByRole('heading', { name: 'AI 订阅服务', exact: true })).toBeVisible()
    const productHeadingFontSize = await page.locator('.page-heading h1').evaluate(
      (element) => Number.parseFloat(getComputedStyle(element).fontSize),
    )
    expect(productHeadingFontSize).toBeLessThanOrEqual(28)
    const firstProductCard = page.locator('.product-card').first()
    await expect(firstProductCard).toBeVisible()
    await expect(firstProductCard).toHaveAttribute('href', /^\/products\//)
    await expect(firstProductCard.locator('.product-card__cta')).toHaveText('查看详情 →')
    await expect(page.locator('.logo__mark')).toHaveText('R')
    const firstProductHref = await firstProductCard.getAttribute('href')
    expect(firstProductHref).not.toBeNull()

    const productDetailResponse = await page.goto(firstProductHref!)
    expect(productDetailResponse?.status()).toBe(200)
    await expectNoHorizontalOverflow(page)
    await expectResponsiveNavigation(page, testInfo.project.name)
    await expect(page.locator('.product-detail')).toBeVisible()
    await expect(page.getByRole('link', { name: '立即订购', exact: true })).toBeVisible()

    const withdrawResponse = await request.post(`/admin/api/articles/${articleId}/withdraw`, {
      headers: authorization,
    })
    expect(withdrawResponse.ok()).toBeTruthy()
    articleWithdrawn = true

    const missingResponse = await page.goto(`/articles/${slug}`)
    expect(missingResponse?.status()).toBe(404)
    await expectNoHorizontalOverflow(page)
    await expectResponsiveNavigation(page, testInfo.project.name)
    await expect(page).toHaveTitle('页面不存在 | RechargeAi')
    await expect(page.getByRole('heading', { name: '页面不存在' })).toBeVisible()
    const errorCard = page.locator('.error-card')
    await expect(errorCard).toBeVisible()
    await expect(errorCard.getByRole('link', { name: '返回产品列表', exact: true })).toBeVisible()
  } finally {
    if (articleId !== undefined) {
      await cleanupArticle(
        request,
        articleId,
        authorization,
        articlePublished && !articleWithdrawn,
      )
    }
  }
})
