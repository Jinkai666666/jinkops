const API_BASE = '/api';

export const API_ENDPOINTS = {
  auth: {
    login: `${API_BASE}/auth/login`,
    verify: `${API_BASE}/auth/verify`
  },
  users: {
    base: `${API_BASE}/users`,
    page: `${API_BASE}/users/page`
  },
  roles: {
    base: `${API_BASE}/roles`
  },
  permissions: {
    base: `${API_BASE}/permissions`
  },
  rbac: {
    assignUserRoles: `${API_BASE}/rbac/user-role/assign`,
    assignRolePermissions: `${API_BASE}/rbac/role-permission/assign`
  },
  logs: {
    base: `${API_BASE}/logs`,
    search: `${API_BASE}/logs/search`,
    page: `${API_BASE}/logs/page`,
    advanced: `${API_BASE}/logs/search/advanced`
  }
} as const;
