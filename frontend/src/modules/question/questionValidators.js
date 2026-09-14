export function validateQuestion(question) {
  const value = String(question ?? '').trim()
  if (!value) return { valid: false, message: '问题不能为空' }
  if (value.length > 500) return { valid: false, message: '问题不能超过 500 个字符' }
  return { valid: true, value }
}

export function normalizeAnswer(answer) {
  const value = String(answer ?? '')
  if (value.length > 50000) return { valid: false, message: '答案不能超过 50000 个字符' }
  return { valid: true, value }
}
