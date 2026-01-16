<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { pageUsers, deleteUser, createUser } from '../api/users';
import { listRoles } from '../api/roles';
import { assignUserRoles } from '../api/rbac';
import type { Role, User } from '../api/types';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const loading = ref(false);
const users = ref<User[]>([]);
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

const createDialog = ref(false);
const createForm = reactive({
  username: '',
  password: '',
  email: ''
});

const assignDialog = ref(false);
const selectedUser = ref<User | null>(null);
const roleOptions = ref<Role[]>([]);
const selectedRoleIds = ref<number[]>([]);

const fetchUsers = async () => {
  loading.value = true;
  try {
    const data = await pageUsers(pagination.page, pagination.size);
    users.value = data.content;
    pagination.total = data.totalElements;
  } finally {
    loading.value = false;
  }
};

const fetchRoles = async () => {
  if (!auth.hasPermission('sys:role:list')) return;
  try {
    roleOptions.value = await listRoles();
  } catch (e) {
    roleOptions.value = [];
  }
};

const handleCreate = async () => {
  if (!createForm.username || !createForm.password) {
    ElMessage.warning('用户名和密码必填');
    return;
  }
  try {
    const payload: Partial<User> = {
      username: createForm.username,
      password: createForm.password,
      email: createForm.email,
      roles: []
    };
    await createUser(payload);
    ElMessage.success('创建成功');
    createDialog.value = false;
    Object.assign(createForm, { username: '', password: '', email: '' });
    fetchUsers();
  } catch (e) {
    // error handled globally
  }
};

const handleDelete = async (user: User) => {
  await ElMessageBox.confirm(`确认删除用户 ${user.username} ?`, '删除用户', { type: 'warning' });
  await deleteUser(user.username);
  ElMessage.success('已删除');
  fetchUsers();
};

const openAssign = (user: User) => {
  selectedUser.value = user;
  selectedRoleIds.value = user.roles?.map((r) => r.id) || [];
  assignDialog.value = true;
};

const saveAssign = async () => {
  if (!selectedUser.value) return;
  await assignUserRoles({
    userId: selectedUser.value.id,
    roleIds: selectedRoleIds.value
  });
  ElMessage.success('已更新用户角色');
  assignDialog.value = false;
  fetchUsers();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  fetchUsers();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  fetchUsers();
};

onMounted(() => {
  fetchUsers();
  fetchRoles();
});
</script>

<template>
  <div class="users-view">
    <div class="header">
      <div>
        <p class="eyebrow">User Directory</p>
        <h2>用户列表</h2>
      </div>
      <div class="actions">
        <el-button type="primary" @click="createDialog = true">新增用户</el-button>
      </div>
    </div>

    <div class="glass-card table-card">
      <el-table :data="users" stripe v-loading="loading" height="520">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" type="success" style="margin: 2px">
              {{ r.code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限" min-width="220">
          <template #default="{ row }">
            <div class="perm-list">
              <el-tag
                v-for="p in row.roles?.flatMap((r: Role) => r.permissions)"
                :key="p.id"
                size="small"
                effect="plain"
                type="info"
                style="margin: 2px"
              >
                {{ p.code }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" text type="primary" v-permission="'sys:rbac:assign'" @click="openAssign(row)">
                设定角色
              </el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pagination.page"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="createDialog" title="新增用户" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="设定用户角色" width="420px">
      <p class="muted">需要 sys:rbac:assign 权限</p>
      <el-select v-model="selectedRoleIds" multiple filterable placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roleOptions" :key="r.id" :label="r.code" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.users-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.eyebrow {
  margin: 0;
  color: var(--text-color-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

h2 {
  margin: 4px 0 0;
}

.actions {
  display: flex;
  gap: 10px;
}

.table-card {
  padding: 14px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.perm-list {
  display: flex;
  flex-wrap: wrap;
}

.muted {
  color: var(--text-color-muted);
  margin-bottom: 8px;
}
</style>
