import http from './http'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

export interface ArticleSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverImageUrl?: string | null
  status: ArticleStatus
  publishedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ArticleDetail extends ArticleSummary {
  contentMarkdown: string
  contentHtml: string
  seoTitle?: string | null
  seoDescription?: string | null
}

export interface ArticleSavePayload {
  title: string
  slug?: string
  summary?: string
  coverImageUrl?: string
  contentMarkdown: string
  seoTitle?: string
  seoDescription?: string
}

export interface PageResult<T> {
  pageNo: number
  pageSize: number
  total: number
  items: T[]
}

export const listArticles = (params: Record<string, unknown>) =>
  http.get<{ data: PageResult<ArticleSummary> }>('/articles', { params })

export const getArticle = (id: number) =>
  http.get<{ data: ArticleDetail }>(`/articles/${id}`)

export const createArticle = (payload: ArticleSavePayload) =>
  http.post<{ data: ArticleDetail }>('/articles', payload)

export const updateArticle = (id: number, payload: ArticleSavePayload) =>
  http.put<{ data: ArticleDetail }>(`/articles/${id}`, payload)

export const publishArticle = (id: number) =>
  http.post<{ data: ArticleDetail }>(`/articles/${id}/publish`)

export const withdrawArticle = (id: number) =>
  http.post<{ data: ArticleDetail }>(`/articles/${id}/withdraw`)

export const deleteArticle = (id: number) =>
  http.delete(`/articles/${id}`)

export const previewArticle = (contentMarkdown: string) =>
  http.post<{ data: { html: string } }>('/articles/preview', { contentMarkdown })

export async function uploadArticleImage(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<{ data: { url: string } }>('/article-assets', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60_000,
  })
  return data.data.url
}
