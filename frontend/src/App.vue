<script setup>
import { useRouter } from 'vue-router'
import { auth, isAdmin, api, clearSession } from './api'

const router = useRouter()

async function logout() {
  try { await api('/auth/logout', { method: 'POST' }) } catch (e) { /* bỏ qua */ }
  clearSession()
  router.push('/login')
}
</script>

<template>
  <template v-if="auth.token">
    <header class="topbar">
      <div class="brand">Wings <b>Up</b> · Kho</div>
      <nav class="nav">
        <router-link to="/">Tổng quan</router-link>
        <router-link to="/products">Sản phẩm</router-link>
        <router-link to="/transactions">Phiếu nhập/xuất</router-link>
        <template v-if="isAdmin()">
          <router-link to="/categories">Danh mục</router-link>
          <router-link to="/suppliers">Nhà cung cấp</router-link>
          <router-link to="/users">Người dùng</router-link>
        </template>
      </nav>
      <div class="who">
        <router-link to="/profile">{{ auth.user?.fullName || auth.user?.username }} ({{ auth.user?.role }})</router-link>
        <a href="#" @click.prevent="logout">Đăng xuất</a>
      </div>
    </header>
    <main><router-view :key="$route.fullPath" /></main>
  </template>
  <router-view v-else />
</template>
