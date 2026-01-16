import { apiDelete, apiGet, apiPost } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { Permission, PermissionCreateRequest } from './types';

export function listPermissions() {
  return apiGet<Permission[]>(API_ENDPOINTS.permissions.base);
}

export function createPermission(payload: PermissionCreateRequest) {
  return apiPost<Permission>(API_ENDPOINTS.permissions.base, payload);
}

export function deletePermission(id: number) {
  return apiDelete<string>(`${API_ENDPOINTS.permissions.base}/${id}`);
}
