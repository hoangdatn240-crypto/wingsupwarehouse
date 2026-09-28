<script setup>
import { ref, onMounted } from 'vue'
import { api, auth, isAdmin, dateTime } from '../api'

const items = ref([])
const products = ref([])
const error = ref('')
const success = ref('')
const form = ref({ productId: '', type: 'IN', quantity: 1, note: '' })

const statusText = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' }

async function load() {
  items.value = await api('/transactions')
  products.value = await api('/products')
}

onMounted(() => load().catch((e) => (error.value = e.message)))

async function run(fn, okMessage) {
  error.value = ''
  success.value = ''
  try { await fn(); success.value = okMessage; await load() }
  catch (e) { error.value = e.message }
}

const create = () => run(async () => {
  await api('/transactions', { method: 'POST', body: form.value })
  form.value = { productId: '', type: form.value.type, quantity: 1, note: '' }
}, isAdmin() ? 'Đã tạo phiếu và cập nhật tồn kho' : 'Đã gửi phiếu, chờ quản trị viên duyệt')

const approve = (t) => run(() => api('/transactions/' + t.id + '/approve', { method: 'POST' }), 'Đã duyệt phiếu')
const reject = (t) => run(() => api('/transactions/' + t.id + '/reject', { method: 'POST' }), 'Đã từ chối phiếu')
const cancel = (t) => confirm('Hủy phiếu này?') && run(() => api('/transactions/' + t.id, { method: 'DELETE' }), 'Đã hủy phiếu')
const canCancel = (t) => t.status === 'PENDING' && (isAdmin() || t.createdBy?.id === auth.user.id)
</script>

<template>
  <h1>Phiếu nhập/xuất kho</h1>
  <div v-if="error" class="msg error">{{ error }}</div>
  <div v-if="success" class="msg success">{{ success }}</div>

  <div class="panel">
    <h2>Tạo phiếu mới</h2>
    <div class="form">
      <label>Loại phiếu
        <select v-model="form.type"><option value="IN">Nhập kho</option><option value="OUT">Xuất kho</option></select>
      </label>
      <label>Sản phẩm
        <select v-model="form.productId">
          <option value="">— Chọn sản phẩm —</option>
          <option v-for="p in products" :key="p.id" :value="p.id">{{ p.sku }} – {{ p.name }} (tồn {{ p.quantity }})</option>
        </select>
      </label>
      <label>Số lượng <input v-model.number="form.quantity" type="number" min="1" /></label>
      <label>Ghi chú <input v-model="form.note" /></label>
      <button class="btn" @click="create">Tạo phiếu</button>
    </div>
  </div>

  <div class="panel table-wrap">
    <div v-if="!items.length" class="empty">Chưa có phiếu nào.</div>
    <table v-else>
      <thead>
        <tr><th>Thời gian</th><th>Loại</th><th>Sản phẩm</th><th class="num">SL</th><th>Người tạo</th><th>Ghi chú</th><th>Trạng thái</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="t in items" :key="t.id">
          <td>{{ dateTime(t.createdAt) }}</td>
          <td><span class="badge" :class="t.type">{{ t.type === 'IN' ? 'Nhập' : 'Xuất' }}</span></td>
          <td>{{ t.product.name }}</td>
          <td class="num">{{ t.quantity }}</td>
          <td>{{ t.createdBy?.fullName || t.createdBy?.username }}</td>
          <td>{{ t.note }}</td>
          <td><span class="badge" :class="t.status">{{ statusText[t.status] }}</span></td>
          <td>
            <div class="actions">
              <template v-if="isAdmin() && t.status === 'PENDING'">
                <button class="btn ok small" @click="approve(t)">Duyệt</button>
                <button class="btn danger small" @click="reject(t)">Từ chối</button>
              </template>
              <button v-if="canCancel(t)" class="btn ghost small" @click="cancel(t)">Hủy phiếu</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
