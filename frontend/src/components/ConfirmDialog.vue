<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  danger: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})
defineEmits(['cancel', 'confirm'])
</script>

<template>
  <div v-if="open" class="dialog-backdrop" @click.self="$emit('cancel')">
    <div class="dialog confirm-dialog">
      <div class="dialog-heading">
        <h3>{{ title }}</h3>
        <button class="icon-btn" type="button" @click="$emit('cancel')">×</button>
      </div>
      <p>{{ message }}</p>
      <div class="dialog-actions">
        <button class="secondary-btn" type="button" :disabled="busy" @click="$emit('cancel')">取消</button>
        <button :class="danger ? 'danger-btn' : 'primary-btn'" type="button" :disabled="busy" @click="$emit('confirm')">
          {{ busy ? '处理中...' : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>
