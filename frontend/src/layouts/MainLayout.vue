<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { UserFilled, Document, Compass, List } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const menus = [
  { label: '能力总览', path: '/overview', icon: Compass, perm: null },
  { label: '用户管理', path: '/users', icon: UserFilled, perm: null },
  { label: '角色管理', path: '/roles', icon: List, perm: 'SYS:ROLE:LIST' },
  { label: '权限管理', path: '/permissions', icon: Document, perm: 'SYS:PERM:LIST' },
  { label: '操作日志', path: '/logs', icon: Document, perm: null }
];

const allowedMenus = computed(() => menus.filter((m) => !m.perm || auth.hasPermission(m.perm)));

const onSelect = (key: string) => router.push(key);
const logout = () => {
  auth.logout();
  router.push('/login');
};

const roleCodes = computed(() => auth.user?.roles?.map((r) => r.code) || []);
</script>

<template>
  <div class="layout page">
    <div class="brand glass-card">
      <div>
        <p class="eyebrow">JinkOps</p>
        <h1>后端能力可视演示台</h1>
      </div>
      <div class="user-meta">
        <div class="who">
          <el-icon><UserFilled /></el-icon>
          <span>{{ auth.username || '未登录' }}</span>
        </div>
        <div class="roles">
          <el-tag v-for="r in roleCodes" :key="r" size="small" type="success" effect="dark">{{ r }}</el-tag>
        </div>
        <el-button text size="small" type="primary" @click="logout">退出</el-button>
      </div>
    </div>

    <div class="body glass-card">
      <el-container>
        <el-aside width="220px" class="nav-pane">
          <el-menu
            :default-active="route.path"
            class="nav"
            background-color="transparent"
            text-color="var(--text-color-strong)"
            active-text-color="var(--primary)"
            @select="onSelect"
          >
            <el-menu-item v-for="item in allowedMenus" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>
          <div class="meta">
            <p>权限数：{{ auth.permissions.length }}</p>
            <p class="muted">JWT + RBAC + 缓存 + 审计链路就绪</p>
          </div>
        </el-aside>
        <el-main class="content">
          <RouterView />
        </el-main>
      </el-container>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
}

.eyebrow {
  margin: 0;
  color: var(--text-color-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

h1 {
  margin: 6px 0 0;
  font-size: 22px;
  color: var(--text-color-strong);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.who {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.roles {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.body {
  overflow: hidden;
}

.nav-pane {
  border-right: 1px solid var(--card-border);
  padding: 16px 12px;
}

.nav {
  border: none;
}

.meta {
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-color-muted);
}

.muted {
  color: var(--text-color-muted);
}

.content {
  padding: 18px 24px;
}
</style>
