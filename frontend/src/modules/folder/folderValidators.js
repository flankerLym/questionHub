export function validateFolderName(name, folders = [], currentId = null) {
  const value = String(name ?? '').trim()
  if (!value) return { valid: false, message: '文件夹名称不能为空' }
  if (value.length > 60) return { valid: false, message: '文件夹名称不能超过 60 个字符' }
  const duplicate = folders.some(
    (item) => item.id !== currentId && item.name.trim().toLowerCase() === value.toLowerCase(),
  )
  if (duplicate) return { valid: false, message: '已存在同名文件夹' }
  return { valid: true, value }
}
