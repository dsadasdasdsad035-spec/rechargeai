<script setup lang="ts">
import { RouterLink } from 'vue-router'

withDefaults(
  defineProps<{
    tag?: 'div' | 'a' | 'router-link'
    to?: string
    interactive?: boolean
  }>(),
  { tag: 'div', interactive: false },
)
</script>

<template>
  <component
    :is="tag === 'router-link' ? RouterLink : tag"
    class="card"
    :class="{ 'card--interactive': interactive || tag !== 'div' }"
    :to="tag === 'router-link' ? to : undefined"
  >
    <slot />
  </component>
</template>

<style scoped>
.card {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}

.card--interactive {
  text-decoration: none;
  color: inherit;
  transition:
    border-color var(--duration-normal) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
}

.card--interactive:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.card--interactive:active {
  transform: translateY(0);
}
</style>
