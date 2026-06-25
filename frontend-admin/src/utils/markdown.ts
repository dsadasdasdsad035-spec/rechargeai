import MarkdownIt from 'markdown-it'

/** 教程内容由管理端维护，允许嵌入 video 等 HTML */
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})

export function renderMarkdown(source: string | null | undefined): string {
  if (!source?.trim()) return ''
  return md.render(source)
}
