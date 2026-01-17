<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listRoles, createRole, updateRole, deleteRole } from '../api/roles';
import { listPermissions } from '../api/permissions';
import { assignRolePermissions } from '../api/rbac';
import type { Permission, Role } from '../api/types';
import { useAuthStore } from '../store/auth';

type TreeNode = {
  id?: number | string;
  label: string;
  code?: string;
  children?: TreeNode[];
  isLeaf?: boolean;
};

const loading = ref(false);
const saving = ref(false);
const assigning = ref(false);
const roles = ref<Role[]>([]);
const permissions = ref<Permission[]>([]);
const auth = useAuthStore();
const isAdmin = computed(() => auth.permissions.includes('ROLE_ADMIN'));
const canListRoles = computed(() => isAdmin.value || auth.hasPermission('SYS:ROLE:LIST'));
const canManageRoles = computed(() => isAdmin.value || auth.hasPermission(['SYS:ROLE:CREATE', 'SYS:ROLE:UPDATE', 'SYS:ROLE:DELETE']));
const canAssignPerm = computed(() => isAdmin.value || auth.hasPermission('SYS:RBAC:ASSIGN'));

const pagination = reactive({
  page: 1,
  size: 10
});
const search = ref('');
let searchTimer: number | undefined;

const createDialog = ref(false);
const editDialog = ref(false);
const assignDialog = ref(false);
const permTreeRef = ref();
const createForm = reactive({ code: '' });
const editForm = reactive<{ id: number | null; code: string }>({ id: null, code: '' });
const currentRole = ref<Role | null>(null);
const checkedPermIds = ref<number[]>([]);
const permTree = ref<TreeNode[]>([]);

const debouncedSearch = (val: string) => {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(() => {
    search.value = val;
    pagination.page = 1;
  }, 300);
};

const filteredRoles = computed(() => {
  const keyword = search.value.trim().toLowerCase();
  if (!keyword) return roles.value;
  return roles.value.filter((r) => r.code.toLowerCase().includes(keyword));
});

const pagedRoles = computed(() => {
  const start = (pagination.page - 1) * pagination.size;
  return filteredRoles.value.slice(start, start + pagination.size);
});

const total = computed(() => filteredRoles.value.length);

const fetchAll = async () => {
  if (!canListRoles.value) {
    ElMessage.error('无权限');
    return;
  }
  loading.value = true;
  try {
    const [roleList, permList] = await Promise.all([listRoles(), listPermissions()]);
    roles.value = roleList;
    permissions.value = permList;
    permTree.value = buildPermTree(permList);
  } catch (e: any) {
    if (e?.status === 403) {
      ElMessage.error('无权限');
    }
  } finally {
    loading.value = false;
  }
};

const buildPermTree = (perms: Permission[]): TreeNode[] => {
  const root: Record<string, TreeNode> = {};
  perms.forEach((p) => {
    const parts = p.code.split(':');
    let current = root;
    let path = '';
    parts.forEach((part, idx) => {
      path = path ? `${path}:${part}` : part;
      if (!current[part]) {
        current[part] = { label: part, children: {} as any };
      }
      if (idx === parts.length - 1) {
        current[part].id = p.id;
        current[part].code = p.code;
        current[part].isLeaf = true;
      }
      current = current[part].children as Record<string, TreeNode>;
    });
  });
  const toArray = (nodeMap: Record<string, TreeNode>): TreeNode[] =>
    Object.values(nodeMap).map((n) => ({
      id: n.id ?? n.label,
      label: n.label,
      code: n.code,
      isLeaf: n.isLeaf,
      children: n.children ? toArray(n.children as Record<string, TreeNode>) : undefined
    }));
  return toArray(root);
};

const handleCreate = async () => {
  if (!createForm.code.trim()) {
    ElMessage.warning('请输入角色编码');
    return;
  }
  if (!canManageRoles.value) {
    ElMessage.error('无权限');
    return;
  }
  saving.value = true;
  try {
    await createRole({ code: createForm.code.trim() });
    ElMessage.success('创建成功');
    createDialog.value = false;
    createForm.code = '';
    fetchAll();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  } finally {
    saving.value = false;
  }
};

const openEdit = (role: Role) => {
  if (!canManageRoles.value) {
    ElMessage.error('无权限');
    return;
  }
  editForm.id = role.id;
  editForm.code = role.code;
  editDialog.value = true;
};

const handleEdit = async () => {
  if (!canManageRoles.value) {
    ElMessage.error('无权限');
    return;
  }
  if (!editForm.code.trim() || editForm.id === null) {
    ElMessage.warning('请输入角色编码');
    return;
  }
  saving.value = true;
  try {
    await updateRole(editForm.id, { code: editForm.code.trim() });
    ElMessage.success('更新成功');
    editDialog.value = false;
    fetchAll();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (role: Role) => {
  if (!canManageRoles.value) {
    ElMessage.error('无权限');
    return;
  }
  await ElMessageBox.confirm(`确认删除角色 ${role.code} ?`, '删除角色', { type: 'warning' });
  try {
    await deleteRole(role.id);
    ElMessage.success('已删除');
    fetchAll();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  }
};

const openAssign = (role: Role) => {
  if (!canAssignPerm.value) {
    ElMessage.error('无权限');
    return;
  }
  currentRole.value = role;
  checkedPermIds.value = role.permissions?.map((p) => p.id) || [];
  assignDialog.value = true;
};

const handleAssign = async () => {
  if (!canAssignPerm.value) {
    ElMessage.error('无权限');
    return;
  }
  if (!currentRole.value) return;
  assigning.value = true;
  try {
    await assignRolePermissions({
      roleId: currentRole.value.id,
      permissionIds: checkedPermIds.value
    });
    ElMessage.success('权限已更新');
    assignDialog.value = false;
    fetchAll();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  } finally {
    assigning.value = false;
  }
};

const onTreeCheck = () => {
  const keys = permTreeRef.value?.getCheckedKeys?.() || [];
  checkedPermIds.value = keys as number[];
};

const handlePageChange = (p: number) => {
  pagination.page = p;
};
const handleSizeChange = (s: number) => {
  pagination.size = s;
  pagination.page = 1;
};

onMounted(() => {
  fetchAll();
});
</script>

<template>
  <div class="roles-view page">
    <div class="header glass-card">
      <div>
        <p class="eyebrow">角色管理</p>
        <h2>角色列表</h2>
        <p class="muted">新增 / 编辑 / 删除角色，并配置权限（仅管理员可见）。</p>
      </div>
      <div class="actions">
        <el-input placeholder="搜索角色编码" clearable @input="debouncedSearch" style="width: 220px" />
        <el-button type="primary" v-permission="'sys:role:create'" @click="createDialog = true">新增角色</el-button>
      </div>
    </div>

    <div class="glass-card">
      <el-table :data="pagedRoles" stripe v-loading="loading" height="520">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="角色编码" width="200" />
        <el-table-column label="权限" min-width="260">
          <template #default="{ row }">
            <el-tag v-for="p in row.permissions" :key="p.id" size="small" effect="plain" style="margin: 2px">
              {{ p.code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" text type="primary" v-permission="'sys:role:update'" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="warning"
                v-permission="'sys:rbac:assign'"
                @click="openAssign(row)"
              >
                配置权限
              </el-button>
              <el-button size="small" text type="danger" v-permission="'sys:role:delete'" @click="handleDelete(row)">
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pagination.page"
          :page-sizes="[5, 10, 20]"
          :page-size="pagination.size"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="createDialog" title="新增角色" width="420px">
      <el-form label-width="100px">
        <el-form-item label="角色编码">
          <el-input v-model="createForm.code" placeholder="如 ADMIN" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑角色" width="420px">
      <el-form label-width="100px">
        <el-form-item label="角色编码">
          <el-input v-model="editForm.code" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="配置权限" width="520px">
      <div class="muted">需要 sys:rbac:assign 权限</div>
      <el-tree
        :key="currentRole?.id || 'perm-tree'"
        ref="permTreeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedPermIds"
        :props="{ children: 'children', label: 'label' }"
        default-expand-all
        class="perm-tree"
        @check-change="onTreeCheck"
      >
        <template #default="{ data }">
          <span>{{ data.code || data.label }}</span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.roles-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  align-items: flex-end;
}

.eyebrow {
  margin: 0;
  color: var(--text-color-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

.muted {
  color: var(--text-color-muted);
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.perm-tree {
  max-height: 360px;
  overflow: auto;
  margin-top: 10px;
  --el-tree-node-hover-bg-color: rgba(255, 255, 255, 0.03);
}
</style>
