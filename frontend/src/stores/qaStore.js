import { defineStore } from 'pinia'
import { storageService } from '../services/storageService.js'
import { validateQuestion, normalizeAnswer } from '../modules/question/questionValidators.js'
import { createId } from '../utils/id.js'

export const useQaStore = defineStore('qa', {
  state: () => ({
    items: [],
    selectedId: null,
    keyword: '',
  }),
  getters: {
    selectedItem(state) {
      return state.items.find((item) => item.id === state.selectedId) ?? null
    },
    filteredItems(state) {
      const key = state.keyword.trim().toLowerCase()
      if (!key) return state.items
      return state.items.filter(
        (item) => item.question.toLowerCase().includes(key) || item.answer.toLowerCase().includes(key),
      )
    },
  },
  actions: {
    async load(folderId) {
      this.keyword = ''
      this.items = folderId ? await storageService.getQuestions(folderId) : []
      this.selectedId = this.items[0]?.id ?? null
    },
    select(id) {
      this.selectedId = id
    },
    clear() {
      this.items = []
      this.selectedId = null
      this.keyword = ''
    },
    async create(folderId, question, answer = '') {
      if (!folderId) throw new Error('请先选择文件夹')
      const q = validateQuestion(question)
      const a = normalizeAnswer(answer)
      if (!q.valid) throw new Error(q.message)
      if (!a.valid) throw new Error(a.message)
      const now = Date.now()
      const item = {
        id: createId('qa'),
        folderId,
        question: q.value,
        answer: a.value,
        createdAt: now,
        updatedAt: now,
      }
      await storageService.putQuestion(item)
      this.items.unshift(item)
      this.selectedId = item.id
      return item
    },
    async update(id, changes) {
      const item = this.items.find((entry) => entry.id === id)
      if (!item) throw new Error('问题不存在')
      const q = validateQuestion(changes.question ?? item.question)
      const a = normalizeAnswer(changes.answer ?? item.answer)
      if (!q.valid) throw new Error(q.message)
      if (!a.valid) throw new Error(a.message)
      const updated = {
        ...item,
        question: q.value,
        answer: a.value,
        updatedAt: Date.now(),
      }
      await storageService.putQuestion(updated)
      this.items = this.items
        .map((entry) => (entry.id === id ? updated : entry))
        .sort((x, y) => y.updatedAt - x.updatedAt)
      return updated
    },
    async remove(id) {
      if (!this.items.some((item) => item.id === id)) throw new Error('问题不存在')
      await storageService.deleteQuestion(id)
      this.items = this.items.filter((item) => item.id !== id)
      if (this.selectedId === id) this.selectedId = this.items[0]?.id ?? null
    },
  },
})
