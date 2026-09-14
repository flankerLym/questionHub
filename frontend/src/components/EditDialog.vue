<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  item: { type: Object, default: null },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'submit'])

const question = ref('')
const answer = ref('')

watch(
  () => [props.open, props.item],
  () => {
    if (!props.open) return
    question.value = props.item?.question ?? ''
    answer.value = props.item?.answer ?? ''
  },
  { immediate: true },
)

function submit() {
  emit('submit', { question: question.value, answer: answer.value })
}
</script>

<template>
  <div v-if="open" class="dialog-backdrop" @click.self="$emit('close')">
    <form class="dialog" @submit.prevent="submit">
      <div class="dialog-heading">
        <div>
          <p class="eyebrow">QUESTION</p>
          <h3>{{ mode === 'create' ? '新建问答' : '编辑问答' }}</h3>
        </div>
        <button class="icon-btn" type="button" @click="$emit('close')">×</button>
      </div>

      <label>
        <span>问题</span>
        <textarea v-model="question" maxlength="500" rows="3" placeholder="输入问题..." autofocus required />
      </label>
      <label>
        <span>答案</span>
        <textarea v-model="answer" maxlength="50000" rows="9" placeholder="可先留空，之后再补充答案。" />
      </label>

      <div class="dialog-actions">
        <button class="secondary-btn" type="button" :disabled="saving" @click="$emit('close')">取消</button>
        <button class="primary-btn" type="submit" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>
