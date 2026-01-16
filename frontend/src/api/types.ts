export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface Permission {
  id: number;
  code: string;
}

export interface Role {
  id: number;
  code: string;
  permissions: Permission[];
}

export interface User {
  id: number;
  username: string;
  password: string;
  email: string;
  roles: Role[];
}

export interface LoginResponse {
  token: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface OperationLog {
  id: number;
  username: string;
  operation: string;
  traceId: string;
  className: string;
  methodName: string;
  args: string;
  description: string;
  elapsedTime: number;
  createTime: string;
}

export interface LogQueryRequest {
  page: number;
  size: number;
  keyword?: string;
  startTime?: string;
  endTime?: string;
}

export interface RoleCreateRequest {
  code: string;
}

export interface RoleUpdateRequest {
  code: string;
}

export interface PermissionCreateRequest {
  code: string;
}

export interface AssignUserRolesRequest {
  userId: number;
  roleIds: number[];
}

export interface AssignRolePermissionsRequest {
  roleId: number;
  permissionIds: number[];
}
