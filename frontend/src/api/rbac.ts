import { apiPost } from './client';
import { API_ENDPOINTS } from './endpoints';
import type { AssignRolePermissionsRequest, AssignUserRolesRequest } from './types';

export function assignUserRoles(payload: AssignUserRolesRequest) {
  return apiPost<string>(API_ENDPOINTS.rbac.assignUserRoles, payload);
}

export function assignRolePermissions(payload: AssignRolePermissionsRequest) {
  return apiPost<string>(API_ENDPOINTS.rbac.assignRolePermissions, payload);
}
