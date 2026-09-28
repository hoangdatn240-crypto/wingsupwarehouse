import { createRouter, createWebHistory } from 'vue-router'
import { auth, isAdmin } from '../api'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Products from '../views/Products.vue'
import Transactions from '../views/Transactions.vue'
import CrudPage from '../views/CrudPage.vue'
import Profile from '../views/Profile.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/', component: Dashboard },
  { path: '/products', component: Products },
  { path: '/transactions', component: Transactions },
  { path: '/profile', component: Profile },
  {
    path: '/categories', component: CrudPage, meta: { admin: true },
    props: {
      title: 'Danh mục', endpoint: '/categories',
      fields: [
        { key: 'name', label: 'Tên danh mục', required: true },
        { key: 'description', label: 'Mô tả' }
      ]
    }
  },
  {
    path: '/suppliers', component: CrudPage, meta: { admin: true },
    props: {
      title: 'Nhà cung cấp', endpoint: '/suppliers',
      fields: [
        { key: 'name', label: 'Tên nhà cung cấp', required: true },
        { key: 'phone', label: 'Số điện thoại' },
        { key: 'email', label: 'Email' },
        { key: 'address', label: 'Địa chỉ' }
      ]
    }
  },
  {
    path: '/users', component: CrudPage, meta: { admin: true },
    props: {
      title: 'Người dùng', endpoint: '/users',
      fields: [
        { key: 'username', label: 'Tên đăng nhập', required: true, lockOnEdit: true },
        { key: 'password', label: 'Mật khẩu', type: 'password', hideInTable: true, hint: 'Để trống khi sửa nếu không đổi' },
        { key: 'fullName', label: 'Họ tên' },
        { key: 'email', label: 'Email' },
        { key: 'role', label: 'Vai trò', type: 'select', options: ['USER', 'ADMIN'] },
        { key: 'active', label: 'Đang hoạt động', type: 'checkbox' }
      ]
    }
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.path !== '/login' && !auth.token) return '/login'
  if (to.path === '/login' && auth.token) return '/'
  if (to.meta.admin && !isAdmin()) return '/'
})

export default router
