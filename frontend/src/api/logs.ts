import { apiGet, apiPost } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { LogQueryRequest, OperationLog, Page } from './types';

export function pageLogs(body: LogQueryRequest) {
  return apiPost<Page<OperationLog>>(API_ENDPOINTS.logs.page, body);
}

export function advancedSearchLogs(params: { keyword?: string; startTime?: number; endTime?: number; page?: number; size?: number }) {
  return apiGet<Page<OperationLog>>(API_ENDPOINTS.logs.advanced, { params });
}
