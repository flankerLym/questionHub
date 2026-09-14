<script setup>
defineProps({
  item: { type: Object, required: true },
  active: { type: Boolean, default: false },
})
defineEmits(['select', 'edit', 'delete'])
</script>

<template>
  <div class="question-row" :class="{ active }">
    <button class="question-bubble" type="button" @click="$emit('select', item.id)">
      <span>{{ item.question }}</span>
      <small>{{ new Date(item.updatedAt).toLocaleString() }}</small>
    </button>
    <div class="question-actions">
      <button class="mini-btn" type="button" title="编辑问题" @click="$emit('edit', item)">✎</button>
      <button class="mini-btn danger" type="button" title="删除问题" @click="$emit('delete', item)">×</button>
    </div>
  </div>
</template>
