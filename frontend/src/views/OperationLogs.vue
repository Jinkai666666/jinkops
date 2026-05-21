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
const query = reactive({
  keyword: '',
  range: [] as (Date | string)[],
  page: 1,
  size: 10,
  total: 0
});

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
      logs.value = data.content;
      query.total = data.totalElements;
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
  } catch (e: any) {
    if (e?.status === 403) {
      ElMessage.error('無權限');
    } else {
      ElMessage.error('載入日誌失敗');
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
        <p class="eyebrow">日誌 / 搜尋 / 降級</p>
        <h2>操作日誌：分頁、關鍵詞、時間區間</h2>
        <p class="muted">
          所有日誌接口集成：POST /api/logs/page（統一分頁），GET /api/logs/search/advanced（進階搜尋，優先 ES，失敗降級至 DB）。
        </p>
      </div>
      <div class="mode-buttons">
        <el-button :type="mode === 'page' ? 'primary' : 'default'" @click="switchMode('page')">
          統一分頁 · POST /api/logs/page
        </el-button>
        <el-button :type="mode === 'advanced' ? 'primary' : 'default'" @click="switchMode('advanced')">
          高級搜尋 · GET /api/logs/search/advanced
        </el-button>
      </div>
    </div>

    <div class="glass-card table-card">
      <div class="header">
        <div>
          <p class="eyebrow">OPERATION LOGS</p>
          <h3>日誌列表（{{ mode }}）</h3>
        </div>
        <div class="filters">
          <el-input
            v-model="query.keyword"
            placeholder="關鍵詞（用戶 / 操作 / 描述 / 定位 / 參數）"
            clearable
            style="width: 260px"
          />
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            start-placeholder="開始時間"
            end-placeholder="結束時間"
            style="width: 360px"
          />
          <el-button type="primary" :loading="loading" @click="fetchLogs">查詢</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" height="540">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用戶" width="140" />
        <el-table-column prop="operation" label="操作" width="180" />
        <el-table-column prop="description" label="描述" width="200" />
        <el-table-column prop="elapsedTime" label="耗時(ms)" width="110" />
        <el-table-column label="參數" min-width="260">
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
        <el-table-column prop="createTime" label="時間" width="180">
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
          當前模式: {{ mode }} · 已覆蓋接口 /api/logs/page、/api/logs/search/advanced
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
