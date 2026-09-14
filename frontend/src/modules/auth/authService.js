const PASSWORD_HASH = 'a22e9ece2ab512a64dc27badaa811cb7905d3210a57bff20986fde4eb5c0d38e'
const SESSION_KEY = 'question-archive-authenticated'

async function sha256(text) {
  const data = new TextEncoder().encode(text)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

export const authService = {
  isAuthenticated() {
    return sessionStorage.getItem(SESSION_KEY) === '1'
  },

  async login(password) {
    if (!password?.trim()) return false
    const valid = (await sha256(password.trim())) === PASSWORD_HASH
    if (valid) sessionStorage.setItem(SESSION_KEY, '1')
    return valid
  },

  logout() {
    sessionStorage.removeItem(SESSION_KEY)
  },
}
