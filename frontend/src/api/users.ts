import { apiDelete, apiGet, apiPost, apiPut } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { Page, User } from './types';

export function listUsers() {
  return apiGet<User[]>(API_ENDPOINTS.users.base);
}

export function getUserByUsername(username: string) {
  return apiGet<User>(`${API_ENDPOINTS.users.base}/${encodeURIComponent(username)}`);
}

export function createUser(user: Partial<User>) {
  return apiPost<User>(API_ENDPOINTS.users.base, user);
}

export function registerUser(user: Partial<User>) {
  return apiPost<User>(API_ENDPOINTS.users.register, user);
}

export function updateUser(user: Partial<User>) {
  return apiPut<User>(API_ENDPOINTS.users.base, user);
}

export function deleteUser(username: string) {
  return apiDelete<string>(`${API_ENDPOINTS.users.base}/${encodeURIComponent(username)}`);
}

export function pageUsers(page: number, size: number) {
  return apiGet<Page<User>>(API_ENDPOINTS.users.page, {
    params: { page, size }
  });
}
