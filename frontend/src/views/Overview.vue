<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';
import { useAuthStore } from '../store/auth';
import { verify } from '../api/auth';

const router = useRouter();
const auth = useAuthStore();

const tokenVisible = ref(false);
const verifyingToken = ref(false);
const tokenPreview = computed(() => {
  const token = auth.token;
  if (!token) return '暂无 token';
  if (tokenVisible.value) return token;
  return token.length > 12 ? `${token.slice(0, 6)}...${token.slice(-6)}` : token;
});

const onVerifyToken = async () => {
  if (!auth.token) {
    ElMessage.warning('暂无 token，请先登录');
    return;
  }
  verifyingToken.value = true;
  try {
    await verify(auth.token);
    ElMessage.success('当前 token 有效');
  } catch (e: any) {
    if (e?.status === 403) {
      ElMessage.error('无权限');
    } else {
      ElMessage.error('Token 无效或已过期');
    }
  } finally {
    verifyingToken.value = false;
  }
};

const copyToken = async () => {
  if (!auth.token) {
    ElMessage.warning('暂无 token');
    return;
  }
  if (!navigator.clipboard?.writeText) {
    ElMessage.warning('当前环境不支持自动复制');
    return;
  }
  try {
    await navigator.clipboard.writeText(auth.token);
    ElMessage.success('Token 已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败，请手动选择');
  }
};

const capabilities = [
  { title: 'JWT 登录 / 校验', detail: 'Token 生成、续期、过期 401 统一处理', tags: ['Auth', 'Security'] },
  { title: 'RBAC 权限模型', detail: 'User/Role/Permission + 自定义注解 + AOP 校验', tags: ['RBAC'] },
  { title: '用户 / 角色 / 权限管理', detail: 'CRUD + 分页 + 角色分配 + 权限绑定', tags: ['CRUD'] },
  { title: '操作日志', detail: '分页、关键词、时间区间查询，traceId 贯通', tags: ['审计', '追踪'] },
  { title: 'Redis 缓存', detail: '用户与权限缓存，分页缓存 + 空值缓存 + 随机 TTL', tags: ['Cache'] },
  { title: '分布式锁', detail: 'Redisson 单机/集群均可，关键写路径加锁', tags: ['Lock'] },
  { title: 'RabbitMQ', detail: '操作日志异步投递，ACK / 重试 / 死信', tags: ['MQ'] },
  { title: 'Quartz', detail: '只读定时扫描，独立于核心写入链路', tags: ['Scheduler'] },
  { title: 'Elasticsearch + MySQL 降级', detail: '日志搜索优先 ES，异常时自动回落 MySQL', tags: ['Search', 'Fallback'] },
  { title: '健康与可观测性', detail: 'Actuator 暴露，级别化日志模板，traceId 打印', tags: ['Ops'] }
];

const flows = [
  { title: '鉴权链路', items: ['登录获取 JWT', '携带 Authorization Bearer', 'Token 解析 + 权限注解 AOP', '401/403 统一返回'] },
  { title: '写操作保护', items: ['Redisson 分布式锁', 'Hikari 连接池', '权限校验 + 操作日志切面', 'MQ 异步出栈'] },
  { title: '审计与搜索', items: ['OperationLog 入库', 'ElasticSearch 搜索', '故障自动降级 MySQL', '分页/关键词/时间区间'] }
];

const quickLinks = [
  { title: '用户 / 角色 / 权限', desc: 'RBAC + 缓存 + 分布式锁演示', action: () => router.push('/users') },
  { title: '操作日志', desc: 'ES 搜索 + 降级 + traceId', action: () => router.push('/logs') }
];
</script>

<template>
  <div class="overview page">
    <section class="hero glass-card">
      <div class="hero-copy">
        <p class="eyebrow">JinkOps · 后端能力总览</p>
        <h1>JWT + RBAC + 缓存 + MQ + ES，一站式可视演示</h1>
        <p class="muted">
          覆盖鉴权、权限、缓存、锁、日志、MQ、定时任务、搜索降级等 10 项后端能力，前端界面一一对标。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/users')">进入用户 / 权限</el-button>
          <el-button size="large" @click="router.push('/logs')">查看操作日志</el-button>
        </div>
        <div class="badges">
          <span class="pill">JWT</span>
          <span class="pill">RBAC</span>
          <span class="pill">Redis 缓存</span>
          <span class="pill">Redisson 锁</span>
          <span class="pill">RabbitMQ</span>
          <span class="pill">Quartz</span>
          <span class="pill">ElasticSearch</span>
        </div>
      </div>
      <div class="hero-panel">
        <div class="panel-card token-card">
          <div class="token-head">
            <p class="panel-title">Token 状态</p>
            <el-tag size="small" effect="dark" :type="auth.token ? 'success' : 'info'">
              {{ auth.token ? '已登录' : '未登录' }}
            </el-tag>
          </div>
          <div class="token-value" :class="{ empty: !auth.token }">
            <code>{{ tokenPreview }}</code>
            <el-button
              v-if="auth.token"
              text
              size="small"
              @click="tokenVisible = !tokenVisible"
            >
              {{ tokenVisible ? '隐藏' : '显示完整' }}
            </el-button>
          </div>
          <div class="token-actions">
            <el-button size="small" :disabled="!auth.token" @click="copyToken">复制 token</el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="!auth.token"
              :loading="verifyingToken"
              @click="onVerifyToken"
            >
              校验 token
            </el-button>
          </div>
          <p class="token-hint muted">校验逻辑已移出登录页，基于 /api/auth/verify。</p>
        </div>

        <div class="panel-card">
          <p class="panel-title">快速入口</p>
          <div class="quick-links">
            <div v-for="link in quickLinks" :key="link.title" class="quick-card" @click="link.action">
              <div>
                <h4>{{ link.title }}</h4>
                <p class="muted">{{ link.desc }}</p>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
        <div class="panel-card">
          <p class="panel-title">链路状态</p>
          <div class="status-grid">
            <div class="status">
              <span class="dot ok"></span>
              <div>
                <p>鉴权 & 权限</p>
                <small>JWT 解析 · 注解校验</small>
              </div>
            </div>
            <div class="status">
              <span class="dot ok"></span>
              <div>
                <p>缓存 & 锁</p>
                <small>Redis + Redisson</small>
              </div>
            </div>
            <div class="status">
              <span class="dot ok"></span>
              <div>
                <p>审计链路</p>
                <small>MQ 异步 · ES 搜索</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="glass-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">对标后台能力</p>
          <h3>10 个能力模块</h3>
        </div>
      </div>
      <div class="cap-grid">
        <div v-for="cap in capabilities" :key="cap.title" class="cap-card">
          <h4>{{ cap.title }}</h4>
          <p class="muted">{{ cap.detail }}</p>
          <div class="tags">
            <span v-for="tag in cap.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="glass-card flows">
      <div class="section-head">
        <div>
          <p class="eyebrow">端到端链路</p>
          <h3>从鉴权到审计</h3>
        </div>
      </div>
      <div class="flow-grid">
        <div v-for="flow in flows" :key="flow.title" class="flow-card">
          <h4>{{ flow.title }}</h4>
          <ol>
            <li v-for="step in flow.items" :key="step">{{ step }}</li>
          </ol>
        </div>
      </div>
    </section>

    <section class="glass-card api">
      <div class="section-head">
        <div>
          <p class="eyebrow">接口与场景</p>
          <h3>你可以直接做的事</h3>
        </div>
      </div>
      <div class="api-grid">
        <div class="api-card">
          <h4>身份与权限</h4>
          <ul>
            <li>登录获取 JWT，验证 401/403</li>
            <li>分配角色 / 权限，观察菜单按钮联动</li>
            <li>尝试无权限操作，查看拒绝提示</li>
          </ul>
        </div>
        <div class="api-card">
          <h4>审计与搜索</h4>
          <ul>
            <li>在“操作日志”按关键词/时间段搜索</li>
            <li>观察 traceId 和类/方法定位</li>
            <li>验证 ES 搜索链路（异常时降级 MySQL）</li>
          </ul>
        </div>
        <div class="api-card">
          <h4>缓存 / 锁 / MQ / 定时</h4>
          <ul>
            <li>用户列表分页缓存、空值缓存、防雪崩</li>
            <li>创建用户走分布式锁，避免并发脏写</li>
            <li>操作日志异步写入 MQ，查看消费结果</li>
            <li>Quartz 只读任务演示，互不干扰写链路</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
  padding: 22px 24px;
}

.hero-copy h1 {
  margin: 6px 0 10px;
  font-size: 28px;
}

.muted {
  color: var(--text-color-muted);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin: 14px 0;
  flex-wrap: wrap;
}

.badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--card-border);
  font-size: 12px;
  color: var(--text-color-strong);
}

.hero-panel {
  display: grid;
  gap: 12px;
}

.panel-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 14px;
}

.token-card .token-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.token-value {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px dashed var(--card-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  word-break: break-all;
}

.token-value code {
  color: var(--text-color-strong);
  font-size: 13px;
}

.token-value.empty code {
  color: var(--text-color-muted);
}

.token-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.token-hint {
  margin: 8px 0 0;
  font-size: 12px;
}

.panel-title {
  margin: 0 0 8px;
  color: var(--text-color-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  font-size: 12px;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-card {
  border: 1px solid var(--card-border);
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: border 0.2s, transform 0.2s;
}

.quick-card:hover {
  border-color: var(--primary);
  transform: translateY(-1px);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.status {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--card-border);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot.ok {
  background: #7ce7a4;
  box-shadow: 0 0 0 6px rgba(124, 231, 164, 0.1);
}

.section-head h3 {
  margin: 4px 0 0;
}

.cap-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.cap-card {
  padding: 14px;
  border: 1px solid var(--card-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.cap-card h4 {
  margin: 0 0 6px;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-color-strong);
  font-size: 12px;
  border: 1px solid var(--card-border);
}

.flows .flow-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.flow-card {
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.flow-card h4 {
  margin: 0 0 8px;
}

.flow-card ol {
  margin: 0;
  padding-left: 18px;
  color: var(--text-color-muted);
  display: grid;
  gap: 4px;
}

.api {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.api-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.api-card {
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.api-card h4 {
  margin: 0 0 6px;
}

.api-card ul {
  margin: 0;
  padding-left: 16px;
  color: var(--text-color-muted);
  display: grid;
  gap: 4px;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
