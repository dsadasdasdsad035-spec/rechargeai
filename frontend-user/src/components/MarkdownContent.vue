<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown'

const props = defineProps<{
  source?: string | null
}>()

const html = computed(() => renderMarkdown(props.source))
</script>

<template>
  <div v-if="html" class="markdown-content" v-html="html" />
</template>

<style scoped>
.markdown-content {
  padding: var(--space-sm) var(--space-md);
  font-size: 0.8125rem;
  line-height: 1.65;
  color: var(--color-text-muted);
  background: var(--color-surface-muted, #f5f5f5);
  border-radius: var(--radius-sm);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin: 0.6em 0 0.3em;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-heading);
}

.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.35em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 1.25em;
}

.markdown-content :deep(img) {
  display: block;
  max-width: 100%;
  border-radius: var(--radius-sm);
  margin: 8px 0;
}

.markdown-content :deep(video) {
  display: block;
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--radius-sm);
  margin: 8px 0;
  background: #000;
}

.markdown-content :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 0.92em;
}

.markdown-content :deep(pre) {
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.06);
  overflow-x: auto;
}

.markdown-content :deep(a) {
  color: var(--color-primary);
  word-break: break-all;
}
</style>
