import { defineStore } from 'pinia'
import { storageService } from '../services/storageService.js'
import { validateFolderName } from '../modules/folder/folderValidators.js'
import { createId } from '../utils/id.js'

export const useFolderStore = defineStore('folder', {
  state: () => ({
    folders: [],
    selectedId: null,
  }),
  getters: {
    selectedFolder(state) {
      return state.folders.find((item) => item.id === state.selectedId) ?? null
    },
  },
  actions: {
    async load() {
      this.folders = await storageService.getFolders()
      if (!this.folders.some((item) => item.id === this.selectedId)) {
        this.selectedId = this.folders[0]?.id ?? null
      }
    },
    select(id) {
      this.selectedId = id
    },
    async create(name) {
      const check = validateFolderName(name, this.folders)
      if (!check.valid) throw new Error(check.message)
      const now = Date.now()
      const folder = {
        id: createId('folder'),
        name: check.value,
        sort: this.folders.length,
        createdAt: now,
        updatedAt: now,
      }
      await storageService.putFolder(folder)
      this.folders.push(folder)
      this.selectedId = folder.id
      return folder
    },
    async rename(id, name) {
      const folder = this.folders.find((item) => item.id === id)
      if (!folder) throw new Error('文件夹不存在')
      const check = validateFolderName(name, this.folders, id)
      if (!check.valid) throw new Error(check.message)
      const updated = { ...folder, name: check.value, updatedAt: Date.now() }
      await storageService.putFolder(updated)
      this.folders = this.folders.map((item) => (item.id === id ? updated : item))
      return updated
    },
    async remove(id) {
      if (!this.folders.some((item) => item.id === id)) throw new Error('文件夹不存在')
      await storageService.deleteFolderCascade(id)
      this.folders = this.folders.filter((item) => item.id !== id)
      if (this.selectedId === id) this.selectedId = this.folders[0]?.id ?? null
    },
  },
})
