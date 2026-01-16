import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
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
        redirect: '/users'
      },
      {
        path: '/users',
        name: 'users',
        component: () => import('../views/Users.vue'),
        meta: { title: '用户列表', requiresAuth: true }
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

  if (auth.token && !auth.user && to.meta.requiresAuth) {
    try {
      await auth.bootstrap();
    } catch (e) {
      auth.logout();
      return next('/login');
    }
  }

  if (to.path === '/login' && auth.token) {
    return next('/users');
  }

  next();
});

export default router;
