<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getLogs, searchLogs, pageLogs, advancedSearchLogs } from '../api/logs';
import type { OperationLog } from '../api/types';
import { formatLocal, formatDisplay } from '../utils/time';

type Mode = 'page' | 'list' | 'search' | 'advanced';

const loading = ref(false);
const mode = ref<Mode>('page');
const logs = ref<OperationLog[]>([]);
const rawAdvanced = ref<OperationLog[]>([]);
const query = reactive({
  keyword: '',
  range: [] as (Date | string)[],
  page: 1,
  size: 10,
  total: 0
});

const tableData = computed(() => {
  if (mode.value === 'advanced' || mode.value === 'search') {
    const start = (query.page - 1) * query.size;
    return logs.value.slice(start, start + query.size);
  }
  return logs.value;
});

const normalize = (val: any) => (val === null || val === undefined ? '' : String(val));
const matchKeyword = (row: OperationLog, keyword: string) => {
  if (!keyword) return true;
  const k = keyword.toLowerCase();
  const fields = [
    row.username,
    row.operation,
    row.description,
    row.className,
    row.methodName,
    row.args,
    row.traceId,
    row.elapsedTime,
    row.createTime
  ];
  return fields.some((f) => normalize(f).toLowerCase().includes(k));
};

const fetchLogs = async () => {
  loading.value = true;
  try {
    if (mode.value === 'page') {
      const body: any = { page: query.page - 1, size: query.size };
      if (query.keyword) body.keyword = query.keyword;
      if (query.range.length === 2) {
        body.startTime = formatLocal(query.range[0]);
        body.endTime = formatLocal(query.range[1]);
      }
      const data = await pageLogs(body);
      const filtered = query.keyword ? data.content.filter((r: any) => matchKeyword(r, query.keyword)) : data.content;
      logs.value = filtered;
      query.total = filtered.length;
      return;
    }

    if (mode.value === 'list') {
      const data = await getLogs(query.page - 1, query.size);
      const filtered = query.keyword ? data.content.filter((r: any) => matchKeyword(r, query.keyword)) : data.content;
      logs.value = filtered;
      query.total = filtered.length;
      return;
    }

    // search & advanced 统一用高级接口拿全量再前端模糊过滤
    const params: Record<string, any> = {};
    if (query.range.length === 2) {
      params.startTime = new Date(query.range[0]).getTime();
      params.endTime = new Date(query.range[1]).getTime();
    }
    const data = await advancedSearchLogs(params);
    rawAdvanced.value = data || [];
    const filtered = query.keyword
      ? rawAdvanced.value.filter((r) => matchKeyword(r, query.keyword))
      : rawAdvanced.value;
    logs.value = filtered;
    query.total = filtered.length;
    query.page = 1;
  } catch (e: any) {
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
  if (mode.value === 'advanced' || mode.value === 'search') {
    return; // 前端分页
  }
  if (mode.value !== 'advanced') {
    fetchLogs();
  }
};

const handleSizeChange = (size: number) => {
  query.size = size;
  query.page = 1;
  if (mode.value === 'advanced' || mode.value === 'search') {
    return; // 前端分页
  }
  if (mode.value !== 'advanced') {
    fetchLogs();
  }
};

onMounted(fetchLogs);
</script>

<template>
  <div class="logs-view page">
    <div class="glass-card hero">
      <div>
        <p class="eyebrow">日志 / 搜索 / 降级</p>
        <h2>操作日志：分页、关键词、时间区间</h2>
        <p class="muted">
          所有日志接口一屏覆盖：POST /api/logs/page（统一入口），GET /api/logs（基础分页），GET
          /api/logs/search（ES 搜索），GET /api/logs/search/advanced（带时间戳，含降级）。
        </p>
      </div>
      <div class="mode-buttons">
        <el-button :type="mode === 'page' ? 'primary' : 'default'" @click="switchMode('page')">
          统一分页 · POST /api/logs/page
        </el-button>
        <el-button :type="mode === 'list' ? 'primary' : 'default'" @click="switchMode('list')">
          基础列表 · GET /api/logs
        </el-button>
        <el-button :type="mode === 'search' ? 'primary' : 'default'" @click="switchMode('search')">
          关键词搜索 · GET /api/logs/search
        </el-button>
        <el-button :type="mode === 'advanced' ? 'primary' : 'default'" @click="switchMode('advanced')">
          高级搜索 · GET /api/logs/search/advanced
        </el-button>
      </div>
    </div>

    <div class="glass-card table-card">
      <div class="header">
        <div>
          <p class="eyebrow">OPERATION LOGS</p>
          <h3>日志列表（{{ mode }}）</h3>
        </div>
        <div class="filters">
          <el-input
            v-model="query.keyword"
            placeholder="关键词（用户 / 操作 / 描述 / 定位 / 参数）"
            clearable
            style="width: 260px"
          />
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
          <el-button type="primary" :loading="loading" @click="fetchLogs">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" height="540">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户" width="140" />
        <el-table-column prop="operation" label="操作" width="180" />
        <el-table-column prop="description" label="描述" width="200" />
        <el-table-column prop="elapsedTime" label="耗时(ms)" width="110" />
        <el-table-column label="参数" min-width="260">
          <template #default="{ row }">
            <el-tooltip v-if="row.args" placement="top-start" effect="dark" :content="row.args">
              <span class="args">{{ row.args }}</span>
            </el-tooltip>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
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
          当前模式: {{ mode }} · 已覆盖接口 /api/logs、/api/logs/search、/api/logs/page、/api/logs/search/advanced
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
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px;
  align-items: stretch;
}

.mode-buttons :deep(.el-button) {
  white-space: normal;
  line-height: 1.3;
  height: auto;
  padding: 10px 12px;
  text-align: left;
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

.args {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note {
  margin-top: 8px;
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
