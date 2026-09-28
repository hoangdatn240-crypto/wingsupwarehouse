import { reactive } from 'vue'

export const auth = reactive({
  token: localStorage.getItem('token'),
  user: JSON.parse(localStorage.getItem('user') || 'null')
})

export const isAdmin = () => auth.user?.role === 'ADMIN'

export function setSession(token, user) {
  auth.token = token
  auth.user = user
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
}

export function clearSession() {
  auth.token = null
  auth.user = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth.token) headers.Authorization = 'Bearer ' + auth.token
  const res = await fetch('/api' + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (res.status === 401 && path !== '/auth/login') {
    clearSession()
    window.location.href = '/login'
  }
  if (!res.ok) throw new Error(data?.message || 'Có lỗi xảy ra (' + res.status + ')')
  return data
}

export const money = (n) => new Intl.NumberFormat('vi-VN').format(n || 0) + ' ₫'
export const dateTime = (s) => (s ? new Date(s).toLocaleString('vi-VN') : '')
