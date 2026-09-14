import { storageService } from './storageService.js'

export const ARCHIVE_VERSION = 1

function isString(value) {
  return typeof value === 'string'
}

export function validateArchive(data) {
  if (!data || typeof data !== 'object') return { valid: false, message: '归档文件格式错误' }
  if (data.version !== ARCHIVE_VERSION) return { valid: false, message: `不支持的归档版本：${data.version ?? '未知'}` }
  if (!Array.isArray(data.folders) || !Array.isArray(data.qaItems)) {
    return { valid: false, message: '归档缺少 folders 或 qaItems 数组' }
  }

  const folderIds = new Set()
  const folderNames = new Set()
  for (const folder of data.folders) {
    if (!folder || !isString(folder.id) || !isString(folder.name) || !folder.name.trim() || folder.name.trim().length > 60) {
      return { valid: false, message: '存在无效文件夹数据' }
    }
    const normalizedName = folder.name.trim().toLowerCase()
    if (folderIds.has(folder.id)) return { valid: false, message: '文件夹 ID 重复' }
    if (folderNames.has(normalizedName)) return { valid: false, message: '归档中存在同名文件夹' }
    folderIds.add(folder.id)
    folderNames.add(normalizedName)
  }

  const qaIds = new Set()
  for (const item of data.qaItems) {
    if (!item || !isString(item.id) || !isString(item.folderId) || !isString(item.question) || !isString(item.answer)) {
      return { valid: false, message: '存在无效问答数据' }
    }
    if (!item.question.trim() || item.question.trim().length > 500) return { valid: false, message: '归档中存在无效问题' }
    if (item.answer.length > 50000) return { valid: false, message: '归档中存在过长答案' }
    if (qaIds.has(item.id)) return { valid: false, message: '问题 ID 重复' }
    if (!folderIds.has(item.folderId)) return { valid: false, message: '存在问题引用了不存在的文件夹' }
    qaIds.add(item.id)
  }

  return { valid: true }
}

export async function readArchiveFile(file) {
  if (!file || file.size > 20 * 1024 * 1024) throw new Error('归档文件不能超过 20MB')
  let data
  try {
    data = JSON.parse(await file.text())
  } catch {
    throw new Error('JSON 文件解析失败')
  }
  const result = validateArchive(data)
  if (!result.valid) throw new Error(result.message)
  return data
}

export const archiveService = {
  async exportArchive() {
    const [folders, qaItems] = await Promise.all([
      storageService.getFolders(),
      storageService.getAllQuestions(),
    ])
    const archive = {
      version: ARCHIVE_VERSION,
      exportedAt: Date.now(),
      folders,
      qaItems,
    }
    const blob = new Blob([JSON.stringify(archive, null, 2)], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `question-archive-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 0)
  },

  async importArchive(file) {
    const data = await readArchiveFile(file)
    await storageService.replaceArchive(data.folders, data.qaItems)
    return data
  },
}
