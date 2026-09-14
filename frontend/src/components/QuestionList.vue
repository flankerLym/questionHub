<script setup>
import QuestionBubble from './QuestionBubble.vue'

defineProps({
  folder: { type: Object, default: null },
  items: { type: Array, required: true },
  selectedId: { type: String, default: null },
  keyword: { type: String, default: '' },
})

defineEmits(['select', 'edit', 'delete', 'create', 'update:keyword'])
</script>

<template>
  <section class="panel questions-panel">
    <div class="panel-heading question-heading">
      <div>
        <p class="eyebrow">QUESTIONS</p>
        <h2>{{ folder?.name || '问题列表' }}</h2>
      </div>
      <button class="primary-btn" type="button" :disabled="!folder" @click="$emit('create')">＋ 新建问题</button>
    </div>

    <div class="search-box">
      <span>⌕</span>
      <input
        :value="keyword"
        :disabled="!folder"
        placeholder="搜索问题或答案..."
        aria-label="搜索问题"
        @input="$emit('update:keyword', $event.target.value)"
      />
    </div>

    <div v-if="!folder" class="empty">
      <strong>请先选择文件夹</strong>
      <span>从左侧选择一个文件夹，或新建文件夹。</span>
    </div>

    <div v-else-if="items.length" class="question-list">
      <QuestionBubble
        v-for="item in items"
        :key="item.id"
        :item="item"
        :active="selectedId === item.id"
        @select="$emit('select', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>

    <div v-else class="empty">
      <strong>没有匹配的问题</strong>
      <span>{{ keyword ? '换一个关键词试试。' : '点击“新建问题”开始归档。' }}</span>
    </div>
  </section>
</template>
