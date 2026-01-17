<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../store/auth';
import { register } from '../api/auth';

const auth = useAuthStore();
const router = useRouter();
const form = reactive({
  username: '',
  password: ''
});
const loading = ref(false);
const registering = ref(false);

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    ElMessage.success('登录成功');
    router.push('/overview');
  } catch {
    // 401/403 在拦截器统一处理
  } finally {
    loading.value = false;
  }
};

const onRegister = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  registering.value = true;
  try {
    await register({ username: form.username, password: form.password });
    ElMessage.success('注册成功，请登录');
  } catch (e: any) {
    if (e?.status === 403) {
      ElMessage.error('无权限');
    }
  } finally {
    registering.value = false;
  }
};
</script>

<template>
  <div class="login-page page">
    <div class="hero glass-card">
      <div class="copy">
        <p class="eyebrow">JinkOps Access</p>
        <h2>JWT + RBAC + 缓存链路登录入口</h2>
        <p class="muted">登录后可体验：JWT 校验、权限注解、菜单联动、缓存与分布式锁、日志与搜索降级。</p>
        <div class="pill-row">
          <span class="pill">JWT</span>
          <span class="pill">RBAC</span>
          <span class="pill">Redis 缓存</span>
          <span class="pill">Redisson 锁</span>
          <span class="pill">MQ & ES</span>
        </div>
      </div>
      <div class="form-card">
        <el-form class="login-form" label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" autocomplete="username" placeholder="输入用户名" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" autocomplete="current-password" placeholder="输入密码" />
          </el-form-item>
          <el-space direction="vertical" style="width: 100%">
            <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登录</el-button>
            <el-button :loading="registering" style="width: 100%" @click="onRegister">
              注册（POST /api/auth/register）
            </el-button>
          </el-space>
        </el-form>
        <p class="hint muted">403 会显示“无权限”，按钮也会自动隐藏/禁用（v-permission）。</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero {
  max-width: 880px;
  width: 100%;
  padding: 28px 32px;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
}

.copy h2 {
  margin: 6px 0 8px;
  font-size: 26px;
}

.muted {
  color: var(--text-color-muted);
  margin: 0 0 12px;
}

.eyebrow {
  margin: 0;
  color: var(--primary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-size: 12px;
}

.pill-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--card-border);
  font-size: 12px;
}

.form-card {
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.35);
}

.login-form {
  margin-top: 8px;
}

.hint {
  margin: 10px 0 0;
  font-size: 12px;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
