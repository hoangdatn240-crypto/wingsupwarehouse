<script setup>
import { ref } from 'vue'
import { api, auth } from '../api'

const oldPassword = ref('')
const newPassword = ref('')
const error = ref('')
const success = ref('')

async function change() {
  error.value = ''
  success.value = ''
  try {
    await api('/auth/password', { method: 'PUT', body: { oldPassword: oldPassword.value, newPassword: newPassword.value } })
    success.value = 'Đã đổi mật khẩu'
    oldPassword.value = ''
    newPassword.value = ''
  } catch (e) { error.value = e.message }
}
</script>

<template>
  <h1>Tài khoản</h1>
  <div class="panel">
    <p><b>{{ auth.user.fullName }}</b> · {{ auth.user.username }} · {{ auth.user.role }}</p>
  </div>
  <div class="panel">
    <h2>Đổi mật khẩu</h2>
    <div v-if="error" class="msg error">{{ error }}</div>
    <div v-if="success" class="msg success">{{ success }}</div>
    <div class="form">
      <label>Mật khẩu cũ <input v-model="oldPassword" type="password" /></label>
      <label>Mật khẩu mới (từ 6 ký tự) <input v-model="newPassword" type="password" /></label>
      <button class="btn" @click="change">Đổi mật khẩu</button>
    </div>
  </div>
</template>
