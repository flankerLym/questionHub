<script setup>
import { useAuthStore } from './stores/authStore'
import LoginView from './views/LoginView.vue'
import ArchiveView from './views/ArchiveView.vue'

const authStore = useAuthStore()
</script>

<template>
  <LoginView v-if="!authStore.authenticated" />
  <ArchiveView v-else />
</template>
