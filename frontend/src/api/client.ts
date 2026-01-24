import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResponse } from './types';
import { useAuthStore } from '../store/auth';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const client = axios.create({
  baseURL,
  timeout: 15000
});

client.interceptors.request.use((config) => {
  const auth = (useAuthStore as typeof useAuthStore & { getInstance?: () => ReturnType<typeof useAuthStore> }).getInstance?.();
  const token = auth?.token || localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    const silent = (response.config as any).__silent || response.config.headers?.['X-Silent'];
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      if (!silent) {
        ElMessage.error(payload.message || '請求失敗');
      }
      return Promise.reject(payload);
    }
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const cfg = error.config as AxiosRequestConfig & { __silent?: boolean };
    const silent = cfg?.__silent || cfg?.headers?.['X-Silent'];
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;

    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      if (!silent) {
        ElMessage.error('登入失效，請重新登入');
      }
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    } else if (status === 403) {
      if (!silent) {
        ElMessage.error('無權限執行此操作');
      }
    } else {
      if (!silent) {
        ElMessage.error(message || '請求錯誤');
      }
    }
    return Promise.reject(error);
  }
);

export async function apiGet<T>(url: string, config?: AxiosRequestConfig) {
  const res = await client.get<ApiResponse<T>>(url, config);
  return res.data.data;
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  const res = await client.post<ApiResponse<T>>(url, data, config);
  return res.data.data;
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  const res = await client.put<ApiResponse<T>>(url, data, config);
  return res.data.data;
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig) {
  const res = await client.delete<ApiResponse<T>>(url, config);
  return res.data.data;
}
