<script setup>
import { ref } from 'vue'

const props = defineProps({
  folders: { type: Array, required: true },
  selectedId: { type: String, default: null },
})
const emit = defineEmits(['select', 'create', 'rename', 'delete'])

const newName = ref('')

function submitCreate() {
  if (!newName.value.trim()) return
  emit('create', newName.value)
  newName.value = ''
}

function rename(folder) {
  const name = window.prompt('新的文件夹名称', folder.name)
  if (name !== null) emit('rename', folder.id, name)
}
</script>

<template>
  <aside class="panel sidebar-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">ARCHIVE</p>
        <h2>文件夹</h2>
      </div>
      <span class="count-badge">{{ folders.length }}</span>
    </div>

    <form class="folder-create" @submit.prevent="submitCreate">
      <input v-model="newName" maxlength="60" placeholder="新建文件夹" aria-label="新建文件夹名称" />
      <button class="icon-btn primary" type="submit" title="新增文件夹">＋</button>
    </form>

    <div v-if="folders.length" class="folder-list">
      <div
        v-for="folder in folders"
        :key="folder.id"
        class="folder-row"
        :class="{ active: selectedId === folder.id }"
      >
        <button class="folder-main" type="button" @click="$emit('select', folder.id)">
          <span class="folder-icon">▰</span>
          <span class="folder-name">{{ folder.name }}</span>
        </button>
        <div class="folder-actions">
          <button class="mini-btn" type="button" title="重命名" @click="rename(folder)">✎</button>
          <button class="mini-btn danger" type="button" title="删除" @click="$emit('delete', folder)">×</button>
        </div>
      </div>
    </div>

    <div v-else class="empty compact">
      <strong>还没有文件夹</strong>
      <span>在上方输入名称创建第一个分类。</span>
    </div>
  </aside>
</template>
