<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  item: { type: Object, default: null },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save'])

const editing = ref(false)
const draft = ref('')

watch(
  () => props.item,
  (item) => {
    editing.value = false
    draft.value = item?.answer ?? ''
  },
  { immediate: true },
)

function startEdit() {
  draft.value = props.item?.answer ?? ''
  editing.value = true
}

function cancel() {
  draft.value = props.item?.answer ?? ''
  editing.value = false
}

function save() {
  if (!props.item) return
  emit('save', props.item.id, draft.value, (ok) => {
    if (ok) editing.value = false
  })
}
</script>

<template>
  <section class="panel answer-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">ANSWER</p>
        <h2>答案</h2>
      </div>
      <button v-if="item && !editing" class="secondary-btn" type="button" @click="startEdit">编辑</button>
    </div>

    <div v-if="!item" class="empty answer-empty">
      <div class="empty-mark">?</div>
      <strong>选择一个问题</strong>
      <span>答案会在这里显示。</span>
    </div>

    <template v-else>
      <div class="selected-question">
        <span>当前问题</span>
        <strong>{{ item.question }}</strong>
      </div>

      <textarea
        v-if="editing"
        v-model="draft"
        class="answer-editor"
        maxlength="50000"
        placeholder="输入答案..."
        autofocus
      />
      <div v-else class="answer-content" :class="{ placeholder: !item.answer }">
        {{ item.answer || '暂无答案，点击“编辑”补充内容。' }}
      </div>

      <div v-if="editing" class="editor-actions">
        <button class="secondary-btn" type="button" :disabled="saving" @click="cancel">取消</button>
        <button class="primary-btn" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存答案' }}
        </button>
      </div>
    </template>
  </section>
</template>
