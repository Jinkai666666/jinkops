<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { pageLogs } from '../api/logs';
import type { OperationLog } from '../api/types';
import { formatLocal, formatDisplay } from '../utils/time';

const loading = ref(false);
const logs = ref<OperationLog[]>([]);
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
    const body: any = {
      page: query.page - 1,
      size: query.size
    };
    if (query.keyword) {
      body.keyword = query.keyword;
    }
    if (query.range && query.range.length === 2) {
      body.startTime = formatLocal(query.range[0]);
      body.endTime = formatLocal(query.range[1]);
    }
    const data = await pageLogs(body);
    logs.value = data.content;
    query.total = data.totalElements;
  } catch (e) {
    ElMessage.error('加载日志失败');
  } finally {
    loading.value = false;
  }
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
  <div class="logs-view">
    <div class="header">
      <div>
        <p class="eyebrow">Operation Logs</p>
        <h2>操作日志分页</h2>
      </div>
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="关键字（用户名/操作）" clearable />
        <el-date-picker
          v-model="query.range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
        <el-button type="primary" @click="fetchLogs">查询</el-button>
        <el-button text @click="resetFilters">重置</el-button>
      </div>
    </div>

    <div class="glass-card table-card">
      <el-table :data="logs" stripe v-loading="loading" height="540">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="operation" label="操作" width="180" />
        <el-table-column prop="description" label="描述" width="200" />
        <el-table-column prop="elapsedTime" label="耗时(ms)" width="100" />
        <el-table-column label="参数" min-width="220">
          <template #default="{ row }">
            <el-tooltip placement="top-start" effect="dark" :content="row.args">
              <span class="args">{{ row.args }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="位置" min-width="200">
          <template #default="{ row }">
            <div class="muted">{{ row.className }}#{{ row.methodName }}</div>
            <div class="muted">traceId: {{ row.traceId }}</div>
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
        <p class="muted">接口映射：POST /api/logs/page，body: { page(从0开始), size, keyword?, startTime?, endTime? }</p>
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

h2 {
  margin: 4px 0 0;
}

.filters {
  display: grid;
  grid-template-columns: 1fr 2fr auto auto;
  gap: 10px;
  align-items: center;
  width: min(880px, 100%);
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

.muted {
  color: var(--text-color-muted);
}

.note {
  margin-top: 8px;
}
</style>
