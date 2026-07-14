<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, EditPen, Plus, Promotion, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteArticle,
  listArticles,
  publishArticle,
  withdrawArticle,
  type ArticleStatus,
  type ArticleSummary,
} from '../services/articleApi'

const router = useRouter()
const articles = ref<ArticleSummary[]>([])
const loading = ref(false)
const title = ref('')
const status = ref<ArticleStatus | ''>('')
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabel: Record<ArticleStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
}

function errorMessage(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? fallback
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

async function load() {
  loading.value = true
  try {
    const { data } = await listArticles({
      title: title.value.trim() || undefined,
      status: status.value || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    articles.value = data.data.items
    total.value = data.data.total
    pageNo.value = data.data.pageNo
    pageSize.value = data.data.pageSize
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章列表加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  pageNo.value = 1
  load()
}

async function publish(row: ArticleSummary) {
  try {
    await publishArticle(row.id)
    ElMessage.success('文章已发布')
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '文章发布失败'))
  }
}

async function withdraw(row: ArticleSummary) {
  try {
    await ElMessageBox.confirm(
      `撤回后，“${row.title}”将立即从公开页面下线。`,
      '撤回文章',
      { confirmButtonText: '确认撤回', cancelButtonText: '保留发布', type: 'warning' },
    )
    await withdrawArticle(row.id)
    ElMessage.success('文章已撤回')
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '文章撤回失败'))
  }
}

async function remove(row: ArticleSummary) {
  try {
    await ElMessageBox.confirm(
      `删除草稿“${row.title}”后无法恢复。`,
      '删除文章',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
    )
    await deleteArticle(row.id)
    ElMessage.success('草稿已删除')
    if (articles.value.length === 1 && pageNo.value > 1) pageNo.value -= 1
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '草稿删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page article-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>文章管理</h2>
        <p>编辑可被搜索引擎直接读取的指南与内容文章</p>
      </div>
      <div class="admin-page__actions">
        <el-button type="primary" :icon="Plus" @click="router.push('/articles/new')">
          新建文章
        </el-button>
      </div>
    </header>

    <section class="article-toolbar" aria-label="文章筛选">
      <el-input
        v-model="title"
        data-testid="article-search"
        placeholder="按标题搜索"
        clearable
        class="article-search"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select
        v-model="status"
        placeholder="全部状态"
        clearable
        class="article-status"
        @change="search"
      >
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
      </el-select>
      <el-button @click="search">查询</el-button>
      <el-button :icon="RefreshLeft" text @click="load">刷新</el-button>
    </section>

    <el-table
      v-loading="loading"
      :data="articles"
      empty-text="暂无文章，点击右上角新建第一篇内容"
      class="article-table"
      style="width: 100%"
    >
      <el-table-column label="文章" min-width="280">
        <template #default="{ row }">
          <button class="article-title" type="button" @click="router.push(`/articles/${row.id}/edit`)">
            <strong>{{ row.title }}</strong>
            <span>/articles/{{ row.slug }}</span>
          </button>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="light">
            {{ statusLabel[row.status as ArticleStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">{{ formatDate(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="最近更新" width="180">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="EditPen" @click="router.push(`/articles/${row.id}/edit`)">
            编辑
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            :icon="Promotion"
            @click="publish(row)"
          >
            发布
          </el-button>
          <el-button v-else link type="warning" :icon="RefreshLeft" @click="withdraw(row)">
            撤回
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            :icon="Delete"
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <footer v-if="total > 0" class="article-pagination">
      <span>共 {{ total }} 篇</span>
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next"
        @current-change="load"
        @size-change="search"
      />
    </footer>
  </div>
</template>

<style scoped>
.article-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px 0 18px;
  border-top: 1px solid var(--wild-border);
}

.article-search {
  width: min(320px, 100%);
}

.article-status {
  width: 150px;
}

.article-table {
  border-top: 1px solid var(--wild-border);
}

.article-title {
  display: grid;
  gap: 5px;
  max-width: 100%;
  padding: 6px 0;
  color: inherit;
  text-align: left;
  background: none;
  border: 0;
  cursor: pointer;
}

.article-title strong {
  color: var(--wild-text-heading);
  font-size: 14px;
  font-weight: 600;
}

.article-title span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-title:hover strong,
.article-title:focus-visible strong {
  color: var(--wild-primary);
}

.article-title:focus-visible {
  outline: 2px solid var(--wild-primary);
  outline-offset: 3px;
  border-radius: 2px;
}

.article-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 640px) {
  .article-status,
  .article-toolbar :deep(.el-button) {
    flex: 1;
  }

  .article-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
