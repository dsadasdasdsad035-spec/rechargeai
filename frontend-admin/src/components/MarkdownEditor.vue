<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '../utils/markdown'
import { uploadTutorialAsset } from '../services/tutorialAssetApi'

const content = defineModel<string>({ default: '' })

defineProps<{
  label: string
  placeholder?: string
  rows?: number
}>()

const activeTab = ref<'edit' | 'preview'>('edit')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const uploadingImage = ref(false)
const uploadingVideo = ref(false)

const previewHtml = computed(() => renderMarkdown(content.value))

function insertText(snippet: string) {
  const el = textareaRef.value
  if (!el) {
    content.value = `${content.value}${snippet}`
    return
  }
  const start = el.selectionStart ?? content.value.length
  const end = el.selectionEnd ?? start
  const before = content.value.slice(0, start)
  const after = content.value.slice(end)
  content.value = `${before}${snippet}${after}`
  requestAnimationFrame(() => {
    el.focus()
    const pos = start + snippet.length
    el.setSelectionRange(pos, pos)
  })
}

function triggerImageUpload() {
  imageInputRef.value?.click()
}

function triggerVideoUpload() {
  videoInputRef.value?.click()
}

async function uploadFile(file: File, kind: 'image' | 'video') {
  const loading = kind === 'image' ? uploadingImage : uploadingVideo
  loading.value = true
  try {
    const { data } = await uploadTutorialAsset(file)
    const url = data.data.url
    if (data.data.kind === 'video' || kind === 'video') {
      insertText(`\n<video src="${url}" controls playsinline preload="metadata"></video>\n`)
      ElMessage.success('视频已上传并插入')
    } else {
      const alt = file.name.replace(/\.[^.]+$/, '')
      insertText(`\n![${alt}](${url})\n`)
      ElMessage.success('图片已上传并插入')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message ?? '上传失败')
  } finally {
    loading.value = false
  }
}

async function onImageChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  await uploadFile(file, 'image')
}

async function onVideoChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('video/')) {
    ElMessage.warning('请选择视频文件（MP4 / WEBM / MOV）')
    return
  }
  await uploadFile(file, 'video')
}
</script>

<template>
  <div class="md-editor">
    <div class="md-editor__toolbar">
      <span class="md-editor__label">{{ label }}</span>
      <div class="md-editor__actions">
        <el-button size="small" :loading="uploadingImage" @click="triggerImageUpload">上传图片</el-button>
        <el-button size="small" :loading="uploadingVideo" @click="triggerVideoUpload">上传视频</el-button>
        <input
          ref="imageInputRef"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          hidden
          @change="onImageChange"
        />
        <input
          ref="videoInputRef"
          type="file"
          accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov"
          hidden
          @change="onVideoChange"
        />
      </div>
    </div>
    <el-tabs v-model="activeTab" class="md-editor__tabs">
      <el-tab-pane label="编辑" name="edit">
        <textarea
          ref="textareaRef"
          v-model="content"
          class="md-textarea"
          :rows="rows ?? 8"
          :placeholder="placeholder ?? '支持 Markdown、图片与视频（上传后自动插入）'"
        />
      </el-tab-pane>
      <el-tab-pane label="预览" name="preview">
        <div v-if="previewHtml" class="md-preview" v-html="previewHtml" />
        <p v-else class="md-preview md-preview--empty">暂无内容</p>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.md-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 8px;
  flex-wrap: wrap;
}

.md-editor__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.md-editor__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.md-preview {
  min-height: 160px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  font-size: 14px;
  line-height: 1.65;
  color: var(--el-text-color-primary);
}

.md-preview--empty {
  color: var(--el-text-color-secondary);
}

.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3) {
  margin: 0.75em 0 0.35em;
  font-size: 1em;
  font-weight: 600;
}

.md-preview :deep(p),
.md-preview :deep(ul),
.md-preview :deep(ol) {
  margin: 0.35em 0;
}

.md-preview :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}

.md-preview :deep(video) {
  display: block;
  max-width: 100%;
  max-height: 360px;
  border-radius: 6px;
  margin: 8px 0;
  background: #000;
}

.md-preview :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 0.92em;
}

.md-preview :deep(pre) {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  overflow-x: auto;
}

.md-preview :deep(a) {
  color: var(--el-color-primary);
}

.md-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  box-sizing: border-box;
}

.md-textarea:focus {
  outline: none;
  border-color: var(--el-color-primary);
}
</style>
