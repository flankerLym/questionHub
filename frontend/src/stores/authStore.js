import { defineStore } from 'pinia'
import { authService } from '../modules/auth/authService.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    authenticated: authService.isAuthenticated(),
    submitting: false,
    error: '',
  }),
  actions: {
    async login(password) {
      this.error = ''
      this.submitting = true
      try {
        const ok = await authService.login(password)
        this.authenticated = ok
        if (!ok) this.error = '访问口令错误'
        return ok
      } finally {
        this.submitting = false
      }
    },
    logout() {
      authService.logout()
      this.authenticated = false
    },
  },
})
