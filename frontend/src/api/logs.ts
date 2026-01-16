import { apiGet, apiPost } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { LogQueryRequest, OperationLog, Page } from './types';

export function getLogs(page = 0, size = 10) {
  return apiGet<Page<OperationLog>>(API_ENDPOINTS.logs.base, {
    params: { page, size }
  });
}

export function searchLogs(keyword: string, page = 0, size = 10) {
  return apiGet<Page<OperationLog>>(API_ENDPOINTS.logs.search, {
    params: { keyword, page, size }
  });
}

export function pageLogs(body: LogQueryRequest) {
  return apiPost<Page<OperationLog>>(API_ENDPOINTS.logs.page, body);
}

export function advancedSearchLogs(params: { keyword?: string; startTime?: number; endTime?: number }) {
  return apiGet<OperationLog[]>(API_ENDPOINTS.logs.advanced, { params });
}
