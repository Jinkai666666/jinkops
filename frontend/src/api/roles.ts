import { apiDelete, apiGet, apiPost, apiPut } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { Role, RoleCreateRequest, RoleUpdateRequest } from './types';

export function listRoles() {
  return apiGet<Role[]>(API_ENDPOINTS.roles.base);
}

export function createRole(payload: RoleCreateRequest) {
  return apiPost<Role>(API_ENDPOINTS.roles.base, payload);
}

export function updateRole(id: number, payload: RoleUpdateRequest) {
  return apiPut<Role>(`${API_ENDPOINTS.roles.base}/${id}`, payload);
}

export function deleteRole(id: number) {
  return apiDelete<string>(`${API_ENDPOINTS.roles.base}/${id}`);
}
