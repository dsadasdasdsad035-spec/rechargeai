<script setup lang="ts">
import type { StatusTone } from '../../utils/statusLabels'

defineProps<{
  label: string
  tone?: StatusTone
}>()
</script>

<template>
  <span class="badge" :class="`badge--${tone ?? 'neutral'}`">{{ label }}</span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
}

.badge--primary {
  background: var(--color-primary-muted);
  color: var(--color-primary-hover);
}

.badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.badge--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.badge--neutral {
  background: var(--color-neutral-bg);
  color: var(--color-neutral);
}
</style>
