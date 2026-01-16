<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const router = useRouter();
const form = reactive({
  username: '',
  password: ''
});
const loading = ref(false);

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    ElMessage.success('登录成功');
    router.push('/users');
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-page page">
    <div class="hero glass-card">
      <div class="copy">
        <p class="eyebrow">JinkOps Access</p>
        <h2>登录后即可测试所有真实接口</h2>
        <p class="muted">JWT 自动携带 · 401/403 统一处理 · 权限联动菜单与按钮</p>
      </div>
      <el-form class="login-form" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" placeholder="输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" placeholder="输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登录</el-button>
      </el-form>
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
  max-width: 520px;
  width: 100%;
  padding: 28px 32px;
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

.login-form {
  margin-top: 8px;
}
</style>
