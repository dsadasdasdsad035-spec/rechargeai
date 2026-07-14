import { expect, test, type Page } from '@playwright/test'

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
    return
  }

  await expect(page.locator('.nav-links')).toBeVisible()
}

test('文章发布后可抓取，撤回后返回真实 404', async ({ page, request }, testInfo) => {
  const username = process.env.E2E_ADMIN_USER ?? 'admin'
  const password = process.env.E2E_ADMIN_PASS ?? 'changeme'
  const unique = `${testInfo.project.name}-${Date.now()}`
  const slug = `seo-${unique}`
  const title = `跨浏览器 SEO 验收 ${unique}`

  const loginResponse = await request.post('/admin/api/auth/login', {
    data: { username, password },
  })
  expect(loginResponse.ok()).toBeTruthy()
  const loginBody = await loginResponse.json()
  expect(loginBody.code).toBe('0')
  const authorization = { Authorization: `Bearer ${loginBody.data.accessToken}` }

  let articleId: number | undefined
  try {
    const createResponse = await request.post('/admin/api/articles', {
      headers: authorization,
      data: {
        title,
        slug,
        summary: '验证服务端渲染、规范链接与结构化数据。',
        contentMarkdown: `# ${title}\n\n这是无需执行 JavaScript 即可读取的正文。`,
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

    const withdrawResponse = await request.post(`/admin/api/articles/${articleId}/withdraw`, {
      headers: authorization,
    })
    expect(withdrawResponse.ok()).toBeTruthy()

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
      await request.post(`/admin/api/articles/${articleId}/withdraw`, {
        headers: authorization,
      }).catch(() => undefined)
      await request.delete(`/admin/api/articles/${articleId}`, {
        headers: authorization,
      }).catch(() => undefined)
    }
  }
})
