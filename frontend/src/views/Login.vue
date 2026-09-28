<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, setSession } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')

async function login() {
  error.value = ''
  try {
    const r = await api('/auth/login', { method: 'POST', body: { username: username.value, password: password.value } })
    setSession(r.token, r.user)
    router.push('/')
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-box">
      <h1>Wings Up – Edu Success</h1>
      <p>Đăng nhập hệ thống quản lý kho</p>
      <div v-if="error" class="msg error">{{ error }}</div>
      <label>Tên đăng nhập <input v-model="username" autocomplete="username" @keyup.enter="login" /></label>
      <label>Mật khẩu <input v-model="password" type="password" autocomplete="current-password" @keyup.enter="login" /></label>
      <button class="btn" @click="login">Đăng nhập</button>
      <div class="hint">Tài khoản mẫu: admin / admin123 · staff / staff123</div>
    </div>
  </div>
</template>
