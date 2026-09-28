<script setup>
import { ref, onMounted } from 'vue'
import { api, isAdmin, money, dateTime } from '../api'

const data = ref(null)
const error = ref('')

onMounted(async () => {
  try { data.value = await api('/dashboard') } catch (e) { error.value = e.message }
})
</script>

<template>
  <h1>Tổng quan kho</h1>
  <div v-if="error" class="msg error">{{ error }}</div>
  <template v-if="data">
    <div class="grid">
      <div class="stat"><span>Số loại sản phẩm</span><strong>{{ data.totalProducts }}</strong></div>
      <div class="stat"><span>Tổng số lượng tồn</span><strong>{{ data.totalQuantity }}</strong></div>
      <div class="stat"><span>Giá trị tồn kho</span><strong>{{ money(data.totalValue) }}</strong></div>
      <div class="stat" :class="{ alert: data.lowStockCount }"><span>Sắp hết hàng</span><strong>{{ data.lowStockCount }}</strong></div>
      <div class="stat"><span>{{ isAdmin() ? 'Phiếu chờ duyệt' : 'Phiếu của tôi đang chờ' }}</span><strong>{{ data.pendingCount }}</strong></div>
    </div>

    <div class="panel">
      <h2>Sản phẩm dưới mức tồn tối thiểu</h2>
      <div v-if="!data.lowStock.length" class="empty">Không có sản phẩm nào dưới mức tối thiểu.</div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th>Mã</th><th>Tên</th><th class="num">Tồn</th><th class="num">Tối thiểu</th></tr></thead>
          <tbody>
            <tr v-for="p in data.lowStock" :key="p.id">
              <td>{{ p.sku }}</td><td>{{ p.name }}</td>
              <td class="num low">{{ p.quantity }}</td><td class="num">{{ p.minQuantity }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="panel">
      <h2>{{ isAdmin() ? 'Phiếu gần đây' : 'Phiếu gần đây của tôi' }}</h2>
      <div v-if="!data.recent.length" class="empty">Chưa có phiếu nào.</div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th>Thời gian</th><th>Loại</th><th>Sản phẩm</th><th class="num">SL</th><th>Trạng thái</th></tr></thead>
          <tbody>
            <tr v-for="t in data.recent" :key="t.id">
              <td>{{ dateTime(t.createdAt) }}</td>
              <td><span class="badge" :class="t.type">{{ t.type === 'IN' ? 'Nhập' : 'Xuất' }}</span></td>
              <td>{{ t.product.name }}</td><td class="num">{{ t.quantity }}</td>
              <td><span class="badge" :class="t.status">{{ t.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</template>
