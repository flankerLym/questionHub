<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

const authStore = useAuthStore()
const password = ref('')

async function submit() {
  const ok = await authStore.login(password.value)
  if (!ok) password.value = ''
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="brand-mark">QA</div>
      <p class="eyebrow">PERSONAL ARCHIVE</p>
      <h1>问题归档系统</h1>
      <p class="login-subtitle">把零散的问题与答案，整理成自己的可检索知识档案。</p>

      <form class="login-form" @submit.prevent="submit">
        <label for="password">访问口令</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          placeholder="请输入访问口令"
          autofocus
        />
        <p v-if="authStore.error" class="form-error">{{ authStore.error }}</p>
        <button class="primary-btn large" type="submit" :disabled="authStore.submitting">
          {{ authStore.submitting ? '验证中...' : '进入归档' }}
        </button>
      </form>
      <p class="login-note">数据仅保存在当前浏览器中，请定期导出 JSON 备份。</p>
    </section>
  </main>
</template>
