<script setup lang="ts">
defineProps<{
  label: string
  type?: string
  placeholder?: string
  modelValue?: string
  required?: boolean
}>()

defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label class="field">
    <span class="field__label">{{ label }}</span>
    <input
      class="field__input"
      :type="type ?? 'text'"
      :placeholder="placeholder"
      :required="required"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </label>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.field__label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-heading);
}

.field__input {
  min-height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: var(--color-surface-raised);
  color: var(--color-text-heading);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.field__input::placeholder {
  color: var(--color-text-subtle);
}

.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-muted);
}
</style>
