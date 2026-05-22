<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { pageLogs, advancedSearchLogs } from '../api/logs';
import type { OperationLog } from '../api/types';
import { formatLocal, formatDisplay } from '../utils/time';

type Mode = 'page' | 'advanced';

const loading = ref(false);
const mode = ref<Mode>('page');
const logs = ref<OperationLog[]>([]);
const tableData = logs;
const queryLatency = ref<number | null>(null);
const query = reactive({
  keyword: '',
  range: [] as (Date | string)[],
  page: 1,
  size: 10,
  total: 0
});

const pickAuditValue = (row: OperationLog, key: string) => {
  const text = `${row.description || ''} ${row.args || ''}`;
  const match = text.match(new RegExp(`${key}=([^;|}\\s]+)`));
  return match?.[1] || '';
};

const redisSourceText = (value: string) => {
  if (!value) return '';
  return value.startsWith('HIT') ? 'Redis' : 'DB';
};

const logSourceText = (row: OperationLog) => {
  // 後端會標這次查詢真正走哪裡，先信它。
  if (row.querySource) return row.querySource;

  // 舊資料沒有 querySource 時，才回頭看審計字串。
  const logSource = pickAuditValue(row, 'logSource');
  if (logSource) return logSource;

  const searchFallback = pickAuditValue(row, 'logSearchFallback');
  const searchSource = pickAuditValue(row, 'logSearchSource');
  if (searchFallback === 'DB') return 'DB';
  if (searchSource === 'ES') return 'ES';

  return mode.value === 'page' ? 'DB' : '-';
};

const sourceItems = (row: OperationLog) => {
  const items: string[] = [];

  const permissionSource = redisSourceText(pickAuditValue(row, 'permissionRedis'));
  if (permissionSource) {
    items.push(`权限: ${permissionSource}`);
  }

  const userSource =
    redisSourceText(pickAuditValue(row, 'userRedis')) ||
    redisSourceText(pickAuditValue(row, 'userPageRedis'));
  if (userSource) {
    items.push(`用户: ${userSource}`);
  }

  const searchFallback = pickAuditValue(row, 'logSearchFallback');
  const mqResend = pickAuditValue(row, 'logSearchMqResend');
  if (mqResend && mqResend !== 'FAILED') {
    items.push('ES补数据: MQ');
  } else if (searchFallback === 'DB') {
    items.push('ES补数据: DB');
  }

  return items;
};

const fetchLogs = async () => {
  loading.value = true;
  const startedAt = performance.now();
  try {
    if (mode.value === 'page') {
      const body: any = { page: query.page - 1, size: query.size };
      if (query.keyword) body.keyword = query.keyword;
      if (query.range.length === 2) {
        body.startTime = formatLocal(query.range[0]);
        body.endTime = formatLocal(query.range[1]);
      }
      const data = await pageLogs(body);
      logs.value = data.content;
      query.total = data.totalElements;
      queryLatency.value = Math.round(performance.now() - startedAt);
      return;
    }

    const params: Record<string, any> = {
      page: query.page - 1,
      size: query.size
    };
    if (query.keyword) params.keyword = query.keyword;
    if (query.range.length === 2) {
      params.startTime = new Date(query.range[0]).getTime();
      params.endTime = new Date(query.range[1]).getTime();
    }
    const data = await advancedSearchLogs(params);
    logs.value = data.content;
    query.total = data.totalElements;
    queryLatency.value = Math.round(performance.now() - startedAt);
  } catch (e: any) {
    queryLatency.value = Math.round(performance.now() - startedAt);
    if (e?.status === 403) {
      ElMessage.error('无权限');
    } else {
      ElMessage.error('加载日志失败');
    }
  } finally {
    loading.value = false;
  }
};

const switchMode = (next: Mode) => {
  mode.value = next;
  query.page = 1;
  fetchLogs();
};

const resetFilters = () => {
  query.keyword = '';
  query.range = [];
  query.page = 1;
  fetchLogs();
};

const handlePageChange = (page: number) => {
  query.page = page;
  fetchLogs();
};

const handleSizeChange = (size: number) => {
  query.size = size;
  query.page = 1;
  fetchLogs();
};

onMounted(fetchLogs);
</script>

<template>
  <div class="logs-view page">
    <div class="glass-card hero">
      <div>
        <h2>操作日志</h2>
        <p class="muted">普通查询：POST /api/logs/page · ES 搜索：GET /api/logs/search/advanced</p>
      </div>
      <div class="mode-buttons">
        <el-button :type="mode === 'page' ? 'primary' : 'default'" @click="switchMode('page')">
          普通查询
        </el-button>
        <el-button :type="mode === 'advanced' ? 'primary' : 'default'" @click="switchMode('advanced')">
          ES 搜索
        </el-button>
      </div>
    </div>

    <div class="glass-card table-card">
      <div class="header">
        <div>
          <p class="eyebrow">OPERATION LOGS</p>
          <h3>日志列表</h3>
        </div>
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="关键词" clearable style="width: 220px" />
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 340px"
          />
          <el-button type="primary" :loading="loading" @click="fetchLogs">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" height="540">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="日志来源" width="100">
          <template #default="{ row }">
            {{ logSourceText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="其他来源" width="180">
          <template #default="{ row }">
            <div v-if="sourceItems(row).length" class="source-list">
              <div v-for="item in sourceItems(row)" :key="item">{{ item }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="140" />
        <el-table-column prop="operation" label="操作" width="180" />
        <el-table-column prop="elapsedTime" label="耗时(ms)" width="110" />
        <el-table-column label="定位" min-width="240">
          <template #default="{ row }">
            <div class="muted">{{ row.className || '-' }}#{{ row.methodName || '-' }}</div>
            <div class="muted">traceId: {{ row.traceId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDisplay(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          :page-size="query.size"
          :total="query.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
      <div class="note">
        <p class="muted">
          当前模式：{{ mode === 'advanced' ? 'ES / DB' : 'DB' }}
          <span class="dot">·</span>
          查询延迟：{{ queryLatency === null ? '-' : `${queryLatency}ms` }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.logs-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
}

.muted {
  color: var(--text-color-muted);
}

.mode-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 8px;
  align-items: stretch;
}

.mode-buttons :deep(.el-button) {
  line-height: 1.3;
  height: auto;
  padding: 10px 12px;
}

.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  margin: 0;
  color: var(--text-color-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.table-card {
  padding: 14px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.note {
  margin-top: 8px;
}

.dot {
  margin: 0 6px;
}

.source-list {
  display: grid;
  gap: 2px;
  line-height: 1.35;
}

@media (max-width: 1080px) {
  .hero {
    flex-direction: column;
  }

  .mode-buttons {
    grid-template-columns: 1fr;
  }

  .filters {
    justify-content: flex-start;
  }
}
</style>
