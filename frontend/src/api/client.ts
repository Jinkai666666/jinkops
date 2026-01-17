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
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      ElMessage.error(payload.message || '请求失败');
      return Promise.reject(payload);
    }
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;

    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      ElMessage.error('登录失效，请重新登录');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    } else if (status === 403) {
      ElMessage.error('无权限执行该操作');
    } else {
      ElMessage.error(message || '请求出错');
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
