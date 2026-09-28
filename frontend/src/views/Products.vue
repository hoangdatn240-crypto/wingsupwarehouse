<script setup>
import { ref, onMounted } from 'vue'
import { api, isAdmin, money } from '../api'

const items = ref([])
const categories = ref([])
const suppliers = ref([])
const q = ref('')
const error = ref('')
const form = ref(null)
const editingId = ref(null)

const blank = () => ({ sku: '', name: '', unit: '', price: 0, quantity: 0, minQuantity: 0, categoryId: '', supplierId: '' })

async function load() {
  try { items.value = await api('/products' + (q.value ? '?q=' + encodeURIComponent(q.value) : '')) }
  catch (e) { error.value = e.message }
}

onMounted(async () => {
  await load()
  categories.value = await api('/categories')
  suppliers.value = await api('/suppliers')
})

function add() { editingId.value = null; form.value = blank(); error.value = '' }
function edit(p) {
  editingId.value = p.id
  form.value = { ...p, categoryId: p.category?.id ?? '', supplierId: p.supplier?.id ?? '' }
  error.value = ''
}

async function save() {
  error.value = ''
  const f = form.value
  const body = {
    ...f,
    category: f.categoryId ? { id: f.categoryId } : null,
    supplier: f.supplierId ? { id: f.supplierId } : null
  }
  try {
    await api(editingId.value ? '/products/' + editingId.value : '/products', {
      method: editingId.value ? 'PUT' : 'POST', body
    })
    form.value = null
    await load()
  } catch (e) { error.value = e.message }
}

async function remove(p) {
  if (!confirm('Xóa sản phẩm "' + p.name + '"?')) return
  try { await api('/products/' + p.id, { method: 'DELETE' }); await load() }
  catch (e) { error.value = e.message }
}
</script>

<template>
  <h1>Sản phẩm</h1>
  <div v-if="error" class="msg error">{{ error }}</div>

  <div class="toolbar">
    <input v-model="q" placeholder="Tìm theo mã hoặc tên sản phẩm" @keyup.enter="load" />
    <button class="btn ghost" @click="load">Tìm</button>
    <button v-if="isAdmin()" class="btn" @click="add">Thêm sản phẩm</button>
  </div>

  <div v-if="form" class="panel">
    <h2>{{ editingId ? 'Sửa sản phẩm' : 'Thêm sản phẩm' }}</h2>
    <div class="form">
      <label>Mã SKU <input v-model="form.sku" /></label>
      <label>Tên sản phẩm <input v-model="form.name" /></label>
      <label>Đơn vị <input v-model="form.unit" /></label>
      <label>Giá <input v-model.number="form.price" type="number" min="0" /></label>
      <label v-if="!editingId">Tồn ban đầu <input v-model.number="form.quantity" type="number" min="0" /></label>
      <label>Tồn tối thiểu <input v-model.number="form.minQuantity" type="number" min="0" /></label>
      <label>Danh mục
        <select v-model="form.categoryId">
          <option value="">— Chưa chọn —</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </label>
      <label>Nhà cung cấp
        <select v-model="form.supplierId">
          <option value="">— Chưa chọn —</option>
          <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </label>
    </div>
    <div class="actions" style="margin-top: 12px">
      <button class="btn" @click="save">Lưu sản phẩm</button>
      <button class="btn ghost" @click="form = null">Hủy</button>
    </div>
  </div>

  <div class="panel table-wrap">
    <div v-if="!items.length" class="empty">Chưa có sản phẩm nào.</div>
    <table v-else>
      <thead>
        <tr>
          <th>Mã</th><th>Tên</th><th>Danh mục</th><th>Nhà cung cấp</th><th>Đơn vị</th>
          <th class="num">Giá</th><th class="num">Tồn</th><th class="num">Tối thiểu</th><th v-if="isAdmin()"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in items" :key="p.id">
          <td>{{ p.sku }}</td><td>{{ p.name }}</td>
          <td>{{ p.category?.name }}</td><td>{{ p.supplier?.name }}</td><td>{{ p.unit }}</td>
          <td class="num">{{ money(p.price) }}</td>
          <td class="num" :class="{ low: p.quantity <= p.minQuantity }">{{ p.quantity }}</td>
          <td class="num">{{ p.minQuantity }}</td>
          <td v-if="isAdmin()">
            <div class="actions">
              <button class="btn ghost small" @click="edit(p)">Sửa</button>
              <button class="btn danger small" @click="remove(p)">Xóa</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
