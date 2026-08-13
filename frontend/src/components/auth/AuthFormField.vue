<script setup lang="ts">
defineProps<{
  id: string
  label: string
  modelValue: string
  type?: string
  autocomplete?: string
  placeholder?: string
  maxlength?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <label :for="id">
    <span>{{ label }}</span>
    <div class="input-wrap">
      <input
        :id="id"
        :value="modelValue"
        :type="type || 'text'"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        :maxlength="maxlength"
        @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
      <slot />
    </div>
  </label>
</template>

<style scoped>
label {
  display: grid;
  min-width: 0;
  gap: 8px;
}

label > span {
  font-size: 13px;
  font-weight: 700;
}

.input-wrap {
  position: relative;
  min-width: 0;
}

input {
  width: 100%;
  height: 48px;
  padding: 0 44px 0 15px;
  border: 1px solid #dfd2ca;
  border-radius: 12px;
  outline: none;
  background: #fffcfa;
  color: var(--text-main);
  font-size: 14px;
  transition: border-color 150ms ease;
}

input:focus {
  border-color: var(--coral);
  box-shadow: 0 0 0 3px rgba(240, 120, 122, 0.1);
}

:deep(button) {
  position: absolute;
  top: 50%;
  right: 10px;
  padding: 5px;
  border: 0;
  background: transparent;
  color: #81736c;
  cursor: pointer;
  font-size: 12px;
  transform: translateY(-50%);
}
</style>
