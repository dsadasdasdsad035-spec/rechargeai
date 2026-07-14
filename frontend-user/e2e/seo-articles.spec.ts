import { expect, test } from '@playwright/test'

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
    await expect(page.getByRole('heading', { name: title, exact: true }).first()).toBeVisible()
    await expect(page.getByText('这是无需执行 JavaScript 即可读取的正文。')).toBeVisible()
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
      'href',
      `https://rechargeai.cn/articles/${slug}`,
    )
    await expect(page.locator('script[type="application/ld+json"]')).toHaveCount(1)
    await expect(page.getByRole('link', { name: '文章中心', exact: true })).toBeVisible()

    const productsResponse = await page.goto('/products')
    expect(productsResponse?.status()).toBe(200)
    const productCards = page.locator('.product-card')
    await expect(productCards.first()).toBeVisible()
    await expect(productCards.first().locator('a[href^="/products/"]')).toBeVisible()

    const withdrawResponse = await request.post(`/admin/api/articles/${articleId}/withdraw`, {
      headers: authorization,
    })
    expect(withdrawResponse.ok()).toBeTruthy()

    const missingResponse = await page.goto(`/articles/${slug}`)
    expect(missingResponse?.status()).toBe(404)
    await expect(page.getByRole('heading', { name: '页面不存在' })).toBeVisible()
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
