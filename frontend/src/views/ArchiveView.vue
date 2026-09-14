<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { useFolderStore } from '../stores/folderStore.js'
import { useQaStore } from '../stores/qaStore.js'
import { storageService } from '../services/storageService.js'
import { archiveService } from '../services/archiveService.js'
import FolderSidebar from '../components/FolderSidebar.vue'
import QuestionList from '../components/QuestionList.vue'
import AnswerPanel from '../components/AnswerPanel.vue'
import EditDialog from '../components/EditDialog.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const authStore = useAuthStore()
const folderStore = useFolderStore()
const qaStore = useQaStore()
const loading = ref(true)
const saving = ref(false)
const notice = ref('')
const error = ref('')
const editOpen = ref(false)
const editMode = ref('create')
const editItem = ref(null)
const confirm = ref({ open: false, title: '', message: '', action: null, danger: true, confirmText: '确认' })
const fileInput = ref(null)

const selectedFolder = computed(() => folderStore.selectedFolder)
const selectedItem = computed(() => qaStore.selectedItem)

let noticeTimer
function showNotice(message) {
  notice.value = message
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => (notice.value = ''), 2600)
}

function showError(err) {
  console.error(err)
  error.value = err instanceof Error ? err.message : String(err)
  setTimeout(() => (error.value = ''), 3500)
}

onMounted(async () => {
  try {
    await storageService.init()
    await folderStore.load()
    if (folderStore.selectedId) await qaStore.load(folderStore.selectedId)
  } catch (err) {
    showError(err)
  } finally {
    loading.value = false
  }
})

watch(
  () => folderStore.selectedId,
  async (id, oldId) => {
    if (loading.value || id === oldId) return
    try {
      await qaStore.load(id)
    } catch (err) {
      showError(err)
    }
  },
)

async function createFolder(name) {
  try {
    await folderStore.create(name)
    showNotice('文件夹已创建')
  } catch (err) {
    showError(err)
  }
}

async function renameFolder(id, name) {
  try {
    await folderStore.rename(id, name)
    showNotice('文件夹已重命名')
  } catch (err) {
    showError(err)
  }
}

function requestDeleteFolder(folder) {
  confirm.value = {
    open: true,
    title: '删除文件夹？',
    message: `将删除“${folder.name}”以及其中的全部问题，此操作无法撤销。`,
    danger: true,
    confirmText: '删除',
    action: async () => {
      await folderStore.remove(folder.id)
      if (!folderStore.selectedId) qaStore.clear()
      showNotice('文件夹已删除')
    },
  }
}

function openCreateQuestion() {
  if (!folderStore.selectedId) return showError(new Error('请先创建或选择文件夹'))
  editMode.value = 'create'
  editItem.value = null
  editOpen.value = true
}

function openEditQuestion(item) {
  editMode.value = 'edit'
  editItem.value = item
  editOpen.value = true
}

async function submitQuestion(payload) {
  saving.value = true
  try {
    if (editMode.value === 'create') {
      await qaStore.create(folderStore.selectedId, payload.question, payload.answer)
      showNotice('问题已创建')
    } else {
      await qaStore.update(editItem.value.id, payload)
      showNotice('问答已更新')
    }
    editOpen.value = false
  } catch (err) {
    showError(err)
  } finally {
    saving.value = false
  }
}

function requestDeleteQuestion(item) {
  confirm.value = {
    open: true,
    title: '删除问题？',
    message: `确定删除“${item.question}”吗？此操作无法撤销。`,
    danger: true,
    confirmText: '删除',
    action: async () => {
      await qaStore.remove(item.id)
      showNotice('问题已删除')
    },
  }
}

async function saveAnswer(id, answer, done) {
  saving.value = true
  try {
    await qaStore.update(id, { answer })
    showNotice('答案已保存')
    done?.(true)
  } catch (err) {
    showError(err)
    done?.(false)
  } finally {
    saving.value = false
  }
}

async function runConfirm() {
  if (!confirm.value.action) return
  saving.value = true
  try {
    await confirm.value.action()
    confirm.value.open = false
  } catch (err) {
    showError(err)
  } finally {
    saving.value = false
  }
}

async function exportData() {
  try {
    await archiveService.exportArchive()
    showNotice('归档文件已导出')
  } catch (err) {
    showError(err)
  }
}

function chooseImport() {
  fileInput.value?.click()
}

async function handleImport(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  confirm.value = {
    open: true,
    title: '导入并覆盖当前数据？',
    message: '导入会覆盖当前文件夹与问答数据。建议先导出备份。',
    danger: false,
    confirmText: '确认导入',
    action: async () => {
      await archiveService.importArchive(file)
      await folderStore.load()
      await qaStore.load(folderStore.selectedId)
      showNotice('数据导入成功')
    },
  }
}
</script>

<template>
  <main class="archive-page">
    <header class="topbar">
      <div class="topbar-brand">
        <div class="brand-mark small">QA</div>
        <div>
          <strong>问题归档系统</strong>
          <span>Local knowledge archive</span>
        </div>
      </div>
      <div class="topbar-actions">
        <input ref="fileInput" class="hidden" type="file" accept="application/json,.json" @change="handleImport" />
        <button class="ghost-btn" type="button" @click="chooseImport">导入</button>
        <button class="ghost-btn" type="button" @click="exportData">导出</button>
        <button class="ghost-btn" type="button" @click="authStore.logout">退出</button>
      </div>
    </header>

    <div v-if="notice" class="toast success">{{ notice }}</div>
    <div v-if="error" class="toast error">{{ error }}</div>

    <section v-if="loading" class="loading-state">正在加载本地归档...</section>

    <section v-else class="workspace">
      <FolderSidebar
        :folders="folderStore.folders"
        :selected-id="folderStore.selectedId"
        @select="folderStore.select"
        @create="createFolder"
        @rename="renameFolder"
        @delete="requestDeleteFolder"
      />

      <QuestionList
        :folder="selectedFolder"
        :items="qaStore.filteredItems"
        :selected-id="qaStore.selectedId"
        :keyword="qaStore.keyword"
        @update:keyword="qaStore.keyword = $event"
        @select="qaStore.select"
        @create="openCreateQuestion"
        @edit="openEditQuestion"
        @delete="requestDeleteQuestion"
      />

      <AnswerPanel :item="selectedItem" :saving="saving" @save="saveAnswer" />
    </section>

    <EditDialog
      :open="editOpen"
      :mode="editMode"
      :item="editItem"
      :saving="saving"
      @close="editOpen = false"
      @submit="submitQuestion"
    />

    <ConfirmDialog
      :open="confirm.open"
      :title="confirm.title"
      :message="confirm.message"
      :confirm-text="confirm.confirmText"
      :danger="confirm.danger"
      :busy="saving"
      @cancel="confirm.open = false"
      @confirm="runConfirm"
    />
  </main>
</template>
