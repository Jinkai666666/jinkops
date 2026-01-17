import { apiGet, apiPost } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { LoginResponse } from './types';

export function login(payload: { username: string; password: string }) {
  return apiPost<LoginResponse>(API_ENDPOINTS.auth.login, payload);
}

export function verify(token: string) {
  return apiGet<string>(API_ENDPOINTS.auth.verify, { params: { token } });
}

export function register(payload: { username: string; password: string; email?: string }) {
  return apiPost(API_ENDPOINTS.auth.register, payload);
}
