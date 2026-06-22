<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'success'
    size?: 'sm' | 'md' | 'lg'
    block?: boolean
    disabled?: boolean
    type?: 'button' | 'submit'
  }>(),
  { variant: 'primary', size: 'md', block: false, disabled: false, type: 'button' },
)
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block }]"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-weight: 500;
  line-height: 1.25;
  transition:
    background var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--block {
  width: 100%;
}

.btn--sm {
  min-height: 36px;
  padding: 0 var(--space-md);
  font-size: 0.875rem;
}

.btn--md {
  min-height: 44px;
  padding: 0 var(--space-lg);
}

.btn--lg {
  min-height: 48px;
  padding: 0 var(--space-xl);
  font-size: 1.0625rem;
}

.btn--primary {
  background: var(--color-primary);
  color: oklch(0.98 0.01 165);
}

.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn--primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn--secondary {
  background: var(--color-surface-raised);
  border-color: var(--color-border-strong);
  color: var(--color-text-heading);
}

.btn--secondary:hover:not(:disabled) {
  background: var(--color-primary-subtle);
  border-color: var(--color-primary-muted);
}

.btn--ghost {
  background: transparent;
  color: var(--color-text);
}

.btn--ghost:hover:not(:disabled) {
  background: var(--color-neutral-bg);
}

.btn--success {
  background: var(--color-success);
  color: oklch(0.98 0.01 145);
}

.btn--success:hover:not(:disabled) {
  background: oklch(0.42 0.12 145);
}
</style>
