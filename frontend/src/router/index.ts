import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../store/auth';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/overview'
      },
      {
        path: '/overview',
        name: 'overview',
        component: () => import('../views/Overview.vue'),
        meta: { title: '能力总览', requiresAuth: true }
      },
      {
        path: '/users',
        name: 'users',
        component: () => import('../views/Users.vue'),
        meta: { title: '用户列表', requiresAuth: true }
      },
      {
        path: '/roles',
        name: 'roles',
        component: () => import('../views/Roles.vue'),
        meta: { title: '角色管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: '/permissions',
        name: 'permissions',
        component: () => import('../views/Permissions.vue'),
        meta: { title: '权限管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: '/logs',
        name: 'logs',
        component: () => import('../views/OperationLogs.vue'),
        meta: { title: '操作日志', requiresAuth: true }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore();

  if (to.meta.requiresAuth && !auth.token) {
    return next('/login');
  }

  if (auth.token && to.meta.requiresAuth) {
    try {
      await auth.bootstrap(true); // 每次進入受保護頁都重新同步使用者與權限
    } catch (e) {
      auth.logout();
      return next('/login');
    }
  }

  if (to.path === '/login' && auth.token) {
    return next('/overview');
  }

  if (to.meta.requiresAdmin) {
    const roles = auth.user?.roles || [];
    const isAdmin = roles.some((r) => (r.code || '').toUpperCase().includes('ADMIN')) || auth.permissions.includes('ROLE_ADMIN');
    if (!isAdmin) {
      ElMessage.error('无权限执行该操作');
      return next('/overview');
    }
  }

  next();
});

export default router;
