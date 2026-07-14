<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Link, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownEditor from '../components/MarkdownEditor.vue'
import {
  createArticle,
  getArticle,
  previewArticle,
  publishArticle,
  updateArticle,
  uploadArticleImage,
  withdrawArticle,
  type ArticleDetail,
  type ArticleSavePayload,
} from '../services/articleApi'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const withdrawing = ref(false)
const article = ref<ArticleDetail | null>(null)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  coverImageUrl: '',
  contentMarkdown: '',
  seoTitle: '',
  seoDescription: '',
})

const articleId = computed(() => {
  const value = Number(route.params.id)
  return Number.isInteger(value) && value > 0 ? value : null
})
const isNew = computed(() => articleId.value === null)
const slugLocked = computed(() => Boolean(article.value?.publishedAt))
const isPublished = computed(() => article.value?.status === 'PUBLISHED')
const publicUrl = computed(() => article.value?.slug ? `/articles/${article.value.slug}` : '')

function errorMessage(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? fallback
}

function applyArticle(detail: ArticleDetail) {
  article.value = detail
  form.title = detail.title
  form.slug = detail.slug
  form.summary = detail.summary ?? ''
  form.coverImageUrl = detail.coverImageUrl ?? ''
  form.contentMarkdown = detail.contentMarkdown
  form.seoTitle = detail.seoTitle ?? ''
  form.seoDescription = detail.seoDescription ?? ''
}

function payload(): ArticleSavePayload {
  return {
    title: form.title.trim(),
    slug: form.slug.trim() || undefined,
    summary: form.summary.trim() || undefined,
    coverImageUrl: form.coverImageUrl.trim() || undefined,
    contentMarkdown: form.contentMarkdown,
    seoTitle: form.seoTitle.trim() || undefined,
    seoDescription: form.seoDescription.trim() || undefined,
  }
}

function validateDraft() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return false
  }
  return true
}

function validatePublish() {
  if (!validateDraft()) return false
  if (!form.contentMarkdown.trim()) {
    ElMessage.warning('请填写文章正文')
    return false
  }
  if (/!\[\s*\]\([^\n)]+\)/.test(form.contentMarkdown)) {
    ElMessage.warning('请为文章图片填写说明文字')
    return false
  }
  return true
}

async function loadArticle() {
  if (!articleId.value) return
  loading.value = true
  try {
    const { data } = await getArticle(articleId.value)
    applyArticle(data.data)
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章加载失败'))
    router.replace('/articles')
  } finally {
    loading.value = false
  }
}

async function save(showSuccess = true): Promise<ArticleDetail | null> {
  if (!validateDraft()) return null
  saving.value = true
  try {
    const response = articleId.value
      ? await updateArticle(articleId.value, payload())
      : await createArticle(payload())
    const saved = response.data.data
    applyArticle(saved)
    if (isNew.value) await router.replace(`/articles/${saved.id}/edit`)
    if (showSuccess) ElMessage.success('文章已保存')
    return saved
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章保存失败'))
    return null
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!validatePublish()) return
  publishing.value = true
  try {
    const saved = await save(false)
    if (!saved) return
    const { data } = await publishArticle(saved.id)
    applyArticle(data.data)
    ElMessage.success('文章已发布，可通过公开链接访问')
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章发布失败'))
  } finally {
    publishing.value = false
  }
}

async function withdraw() {
  if (!article.value) return
  try {
    await ElMessageBox.confirm(
      '撤回后文章将立即从公开页面和站点地图中移除。',
      '撤回文章',
      { confirmButtonText: '确认撤回', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  withdrawing.value = true
  try {
    const { data } = await withdrawArticle(article.value.id)
    applyArticle(data.data)
    ElMessage.success('文章已撤回，可继续编辑')
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章撤回失败'))
  } finally {
    withdrawing.value = false
  }
}

async function previewMarkdown(source: string) {
  const { data } = await previewArticle(source)
  return data.data.html
}

onMounted(loadArticle)
</script>

<template>
  <div v-loading="loading" class="admin-page article-editor-page">
    <header class="editor-header">
      <div>
        <el-button text :icon="ArrowLeft" class="back-button" @click="router.push('/articles')">
          返回文章列表
        </el-button>
        <h2>{{ isNew ? '新建文章' : '编辑文章' }}</h2>
        <p>{{ isPublished ? '正在编辑已发布内容，保存后公开页会同步更新' : '先保存草稿，确认预览无误后再发布' }}</p>
      </div>
      <div class="editor-header__actions">
        <el-button data-testid="article-save" :loading="saving" @click="save()">
          {{ isPublished ? '保存修改' : '保存草稿' }}
        </el-button>
        <el-button
          v-if="!isPublished"
          data-testid="article-publish"
          type="primary"
          :icon="Promotion"
          :loading="publishing"
          @click="publish"
        >
          发布文章
        </el-button>
        <el-button v-else type="warning" plain :loading="withdrawing" @click="withdraw">
          撤回文章
        </el-button>
      </div>
    </header>

    <div class="editor-layout">
      <main class="editor-main">
        <section class="editor-section" aria-labelledby="article-basic-heading">
          <div class="section-heading">
            <span>01</span>
            <div>
              <h3 id="article-basic-heading">文章信息</h3>
              <p>标题用于页面主标题，链接标识决定公开访问地址</p>
            </div>
          </div>

          <el-form label-position="top" class="article-form">
            <el-form-item label="文章标题" required>
              <el-input
                v-model="form.title"
                data-testid="article-title"
                maxlength="200"
                show-word-limit
                placeholder="用清晰、具体的标题说明文章价值"
              />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="链接标识（slug）">
                <el-input
                  v-model="form.slug"
                  data-testid="article-slug"
                  maxlength="180"
                  :disabled="slugLocked"
                  placeholder="留空后自动生成 article-{id}"
                >
                  <template #prepend>/articles/</template>
                </el-input>
                <p v-if="slugLocked" class="field-help">文章首次发布后，链接标识将永久锁定</p>
              </el-form-item>
              <el-form-item label="封面图片地址">
                <el-input v-model="form.coverImageUrl" maxlength="512" placeholder="/api/article-assets/..." />
              </el-form-item>
            </div>
            <el-form-item label="摘要">
              <el-input
                v-model="form.summary"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="留空时将从正文自动生成"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="editor-section editor-section--content" aria-labelledby="article-content-heading">
          <div class="section-heading">
            <span>02</span>
            <div>
              <h3 id="article-content-heading">正文与预览</h3>
              <p>预览内容由服务端按公开页相同规则渲染和过滤</p>
            </div>
          </div>
          <MarkdownEditor
            v-model="form.contentMarkdown"
            label="文章正文（Markdown）"
            :rows="18"
            :allow-video="false"
            :upload-image="uploadArticleImage"
            :preview-markdown="previewMarkdown"
            test-id="article-content"
          />
        </section>

        <section class="editor-section" aria-labelledby="article-seo-heading">
          <div class="section-heading">
            <span>03</span>
            <div>
              <h3 id="article-seo-heading">搜索摘要</h3>
              <p>可选覆盖项；留空时使用文章标题和摘要</p>
            </div>
          </div>
          <el-form label-position="top" class="article-form">
            <el-form-item label="SEO 标题">
              <el-input v-model="form.seoTitle" maxlength="200" show-word-limit placeholder="搜索结果标题" />
            </el-form-item>
            <el-form-item label="SEO 描述">
              <el-input
                v-model="form.seoDescription"
                type="textarea"
                :rows="3"
                maxlength="320"
                show-word-limit
                placeholder="搜索结果描述，建议控制在 160 字内"
              />
            </el-form-item>
          </el-form>
        </section>
      </main>

      <aside class="publish-panel" aria-label="发布状态">
        <span class="publish-panel__eyebrow">发布状态</span>
        <div class="publish-panel__state">
          <span :class="['state-dot', { 'state-dot--published': isPublished }]" />
          <strong>{{ isPublished ? '已公开' : '未发布' }}</strong>
        </div>
        <p v-if="isPublished">搜索引擎和访客现在可以直接读取这篇文章。</p>
        <p v-else>草稿不会出现在文章列表和站点地图中。</p>
        <a v-if="publicUrl" :href="publicUrl" target="_blank" rel="noopener noreferrer" class="public-link">
          <Link />
          <span>{{ isPublished ? '查看公开页面' : '公开路径' }}</span>
        </a>
        <dl v-if="article" class="article-meta">
          <div>
            <dt>最近更新</dt>
            <dd>{{ new Date(article.updatedAt).toLocaleString('zh-CN', { hour12: false }) }}</dd>
          </div>
          <div v-if="article.publishedAt">
            <dt>首次发布</dt>
            <dd>{{ new Date(article.publishedAt).toLocaleString('zh-CN', { hour12: false }) }}</dd>
          </div>
        </dl>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.article-editor-page {
  --editor-line: var(--wild-border);
}

.editor-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--editor-line);
}

.editor-header h2 {
  margin-top: 8px;
}

.editor-header p {
  margin: 7px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.back-button {
  margin-left: -12px;
  color: var(--el-text-color-secondary);
}

.editor-header__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: clamp(28px, 4vw, 56px);
  align-items: start;
  padding-top: 28px;
}

.editor-main {
  min-width: 0;
}

.editor-section {
  padding: 0 0 34px;
}

.editor-section + .editor-section {
  padding-top: 30px;
  border-top: 1px solid var(--editor-line);
}

.section-heading {
  display: flex;
  gap: 14px;
  margin-bottom: 22px;
}

.section-heading > span {
  padding-top: 2px;
  color: var(--wild-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.section-heading h3 {
  font-size: 17px;
}

.section-heading p {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.article-form {
  max-width: 820px;
}

.form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.field-help {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.publish-panel {
  position: sticky;
  top: 24px;
  padding: 4px 0 20px 20px;
  border-left: 1px solid var(--editor-line);
}

.publish-panel__eyebrow {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.publish-panel__state {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 13px;
  color: var(--wild-text-heading);
}

.state-dot {
  width: 9px;
  height: 9px;
  background: #9c958d;
  border-radius: 50%;
}

.state-dot--published {
  background: var(--wild-primary);
  box-shadow: 0 0 0 4px var(--wild-primary-light);
}

.publish-panel > p {
  margin: 12px 0 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.public-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--wild-primary);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.public-link svg {
  width: 15px;
}

.article-meta {
  display: grid;
  gap: 14px;
  margin: 24px 0 0;
  padding-top: 18px;
  border-top: 1px solid var(--editor-line);
}

.article-meta dt {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.article-meta dd {
  margin: 4px 0 0;
  color: var(--wild-text-heading);
  font-size: 12px;
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .publish-panel {
    position: static;
    grid-row: 1;
    padding: 0 0 22px;
    border-left: 0;
    border-bottom: 1px solid var(--editor-line);
  }
}

@media (max-width: 640px) {
  .editor-header {
    align-items: stretch;
    flex-direction: column;
  }

  .editor-header__actions > :deep(.el-button) {
    flex: 1;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
