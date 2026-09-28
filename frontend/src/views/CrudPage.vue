<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const props = defineProps({ title: String, endpoint: String, fields: Array })

const items = ref([])
const form = ref(null)
const editingId = ref(null)
const error = ref('')

const blank = () =>
  Object.fromEntries(props.fields.map((f) => [f.key, f.type === 'checkbox' ? true : f.options ? f.options[0] : '']))

async function load() {
  try { items.value = await api(props.endpoint) } catch (e) { error.value = e.message }
}
onMounted(load)

function add() { editingId.value = null; form.value = blank(); error.value = '' }
function edit(it) {
  editingId.value = it.id
  form.value = { ...blank(), ...it }
  props.fields.filter((f) => f.type === 'password').forEach((f) => (form.value[f.key] = ''))
  error.value = ''
}

async function save() {
  error.value = ''
  try {
    await api(editingId.value ? props.endpoint + '/' + editingId.value : props.endpoint, {
      method: editingId.value ? 'PUT' : 'POST', body: form.value
    })
    form.value = null
    await load()
  } catch (e) { error.value = e.message }
}

async function remove(it) {
  if (!confirm('Xóa mục này?')) return
  try { await api(props.endpoint + '/' + it.id, { method: 'DELETE' }); await load() }
  catch (e) { error.value = e.message }
}

const shown = () => props.fields.filter((f) => !f.hideInTable)
</script>

<template>
  <h1>{{ title }}</h1>
  <div v-if="error" class="msg error">{{ error }}</div>
  <div class="toolbar"><button class="btn" @click="add">Thêm mới</button></div>

  <div v-if="form" class="panel">
    <div class="form">
      <template v-for="f in fields" :key="f.key">
        <label v-if="f.type === 'checkbox'" class="check">
          <input v-model="form[f.key]" type="checkbox" /> {{ f.label }}
        </label>
        <label v-else-if="f.type === 'select'">{{ f.label }}
          <select v-model="form[f.key]"><option v-for="o in f.options" :key="o" :value="o">{{ o }}</option></select>
        </label>
        <label v-else>{{ f.label }}
          <input v-model="form[f.key]" :type="f.type || 'text'" :disabled="f.lockOnEdit && editingId" :placeholder="f.hint" />
        </label>
      </template>
    </div>
    <div class="actions" style="margin-top: 12px">
      <button class="btn" @click="save">Lưu</button>
      <button class="btn ghost" @click="form = null">Hủy</button>
    </div>
  </div>

  <div class="panel table-wrap">
    <div v-if="!items.length" class="empty">Chưa có dữ liệu.</div>
    <table v-else>
      <thead><tr><th v-for="f in shown()" :key="f.key">{{ f.label }}</th><th></th></tr></thead>
      <tbody>
        <tr v-for="it in items" :key="it.id">
          <td v-for="f in shown()" :key="f.key">{{ f.type === 'checkbox' ? (it[f.key] ? 'Có' : 'Không') : it[f.key] }}</td>
          <td>
            <div class="actions">
              <button class="btn ghost small" @click="edit(it)">Sửa</button>
              <button class="btn danger small" @click="remove(it)">Xóa</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
