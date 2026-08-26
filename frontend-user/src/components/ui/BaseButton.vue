<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'success' | 'accent'
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
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-hover));
  color: #fff;
  box-shadow: 0 1px 2px rgb(37 99 235 / 25%), 0 4px 12px rgb(37 99 235 / 18%);
}

.btn--primary:hover:not(:disabled) {
  background: linear-gradient(180deg, #3b82f6, var(--color-primary));
  box-shadow: 0 2px 8px rgb(37 99 235 / 30%);
}

.btn--accent {
  background: linear-gradient(180deg, var(--color-accent), var(--color-accent-hover));
  color: #fff;
  box-shadow: 0 1px 2px rgb(234 88 12 / 25%), 0 4px 12px rgb(234 88 12 / 18%);
}

.btn--accent:hover:not(:disabled) {
  background: linear-gradient(180deg, #f97316, var(--color-accent));
  box-shadow: 0 2px 8px rgb(234 88 12 / 30%);
}

.btn--accent:active:not(:disabled) {
  transform: scale(0.98);
}

.btn--secondary {
  background: var(--color-surface);
  border-color: var(--color-border);
  color: var(--color-text-heading);
  box-shadow: var(--shadow-sm);
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
