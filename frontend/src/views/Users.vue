<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { pageUsers, deleteUser, createUser, updateUser, getUserByUsername, registerUser } from '../api/users';
import { listRoles } from '../api/roles';
import { assignUserRoles } from '../api/rbac';
import type { Role, User } from '../api/types';
import { useAuthStore } from '../store/auth';

const loading = ref(false);
const users = ref<User[]>([]);
const pagination = reactive({ page: 1, size: 10, total: 0 });

const searchKey = ref('');
let searchTimer: number | undefined;

const roleOptions = ref<Role[]>([]);

const createDialog = ref(false);
const registerDialog = ref(false);
const editDialog = ref(false);
const assignDialog = ref(false);

const createForm = reactive({ username: '', password: '', email: '' });
const registerForm = reactive({ username: '', password: '', email: '' });
const editForm = reactive<{ username: string; password: string; email: string }>({
  username: '',
  password: '',
  email: ''
});
const selectedUser = ref<User | null>(null);
const selectedRoleIds = ref<number[]>([]);

const auth = useAuthStore();
const isAdmin = computed(() => {
  const roles = auth.user?.roles || [];
  return (
    roles.some((r) => (r.code || '').toUpperCase().includes('ADMIN')) ||
    auth.permissions.includes('ROLE_ADMIN')
  );
});
const selfUsername = computed(() => auth.user?.username || auth.username || '');

const fetchRoles = async () => {
  if (!isAdmin.value) {
    roleOptions.value = [];
    return;
  }
  try {
    roleOptions.value = await listRoles();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
};

const fetchUsers = async () => {
  loading.value = true;
  try {
    if (isAdmin.value) {
      const data = await pageUsers(pagination.page, pagination.size);
      users.value = data.content;
      pagination.total = data.totalElements;
    } else {
      const username = selfUsername.value;
      if (!username) {
        users.value = [];
        pagination.total = 0;
        return;
      }
      const user = await getUserByUsername(username);
      users.value = user ? [user] : [];
      pagination.total = users.value.length;
      pagination.page = 1;
    }
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  if (!isAdmin.value) {
    fetchUsers();
    return;
  }
  const key = searchKey.value.trim();
  if (!key) {
    fetchUsers();
    return;
  }
  loading.value = true;
  try {
    const user = await getUserByUsername(key);
    users.value = user ? [user] : [];
    pagination.total = user ? 1 : 0;
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  } finally {
    loading.value = false;
  }
};

const onSearchInput = (val: string) => {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(() => {
    searchKey.value = val;
    handleSearch();
  }, 300);
};

const handleCreate = async () => {
  if (!createForm.username || !createForm.password) {
    ElMessage.warning('請填寫帳號與密碼');
    return;
  }
  try {
    await createUser({ ...createForm });
    ElMessage.success('建立成功');
    createDialog.value = false;
    Object.assign(createForm, { username: '', password: '', email: '' });
    fetchUsers();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
};

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('請填寫帳號與密碼');
    return;
  }
  try {
    await registerUser({ ...registerForm });
    ElMessage.success('註冊成功');
    registerDialog.value = false;
    Object.assign(registerForm, { username: '', password: '', email: '' });
    fetchUsers();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
};

const openEdit = (user: User) => {
  if (!isAdmin.value) {
    ElMessage.warning('僅可查看自己的資料');
    return;
  }
  editForm.username = user.username;
  editForm.password = '';
  editForm.email = user.email;
  editDialog.value = true;
};

const handleEdit = async () => {
  if (!editForm.username) {
    ElMessage.warning('請填寫帳號');
    return;
  }
  try {
    await updateUser({ ...editForm });
    ElMessage.success('更新成功');
    editDialog.value = false;
    if (editForm.username === selfUsername.value) {
      await auth.bootstrap(true);
    }
    fetchUsers();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
};

const handleDelete = async (user: User) => {
  if (!isAdmin.value) {
    ElMessage.warning('僅 admin 可刪除用戶');
    return;
  }
  await ElMessageBox.confirm(`刪除用戶 ${user.username} ?`, '刪除用戶', { type: 'warning' });
  try {
    await deleteUser(user.username);
    ElMessage.success('已刪除');
    fetchUsers();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
};

const openAssign = (user: User) => {
  if (!isAdmin.value) {
    ElMessage.error('無權限進行此操作');
    return;
  }
  selectedUser.value = user;
  selectedRoleIds.value = user.roles?.map((r) => r.id) || [];
  assignDialog.value = true;
};

const handleAssign = async () => {
  if (!isAdmin.value) {
    ElMessage.error('無權限進行此操作');
    return;
  }
  if (!selectedUser.value) return;
  try {
    await assignUserRoles({ userId: selectedUser.value.id, roleIds: selectedRoleIds.value });
    ElMessage.success('角色已更新');
    assignDialog.value = false;
    if (selectedUser.value.username === selfUsername.value) {
      await auth.bootstrap(true);
    }
    fetchUsers();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('無權限');
  }
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
  <div class="users-view page">
    <div class="hero glass-card">
      <div>
        <p class="eyebrow">用戶管理</p>
        <h2>列表 / 建立 / 註冊 / 更新 / 刪除 / 分配角色</h2>
        <p class="muted">已涵蓋：/api/users CRUD、/api/users/page、/api/users/{username}、/api/users/register、/api/rbac/user-role/assign</p>
      </div>
      <div class="actions">
        <el-input
          :placeholder="isAdmin ? '輸入帳號查詢' : '僅可查看自己的資料'"
          clearable
          :disabled="!isAdmin"
          @input="onSearchInput"
          style="width: 220px"
        />
        <el-button type="primary" v-permission="'SYS:USER:CREATE'" @click="createDialog = true">新增用戶</el-button>
        <el-button v-permission="'SYS:USER:CREATE'" @click="registerDialog = true">後台註冊</el-button>
      </div>
    </div>

    <el-alert v-if="!isAdmin" type="info" show-icon title="僅可查看自己的資料" class="notice" />

    <div class="glass-card table-card">
      <el-table :data="users" stripe v-loading="loading" height="520">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="帳號" width="160" />
        <el-table-column prop="email" label="Email" />
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" type="success" style="margin: 2px">
              {{ r.code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" text type="primary" v-permission="'SYS:USER:UPDATE'" @click="openEdit(row)">
                編輯
              </el-button>
              <el-button size="small" text type="warning" v-permission="'SYS:RBAC:ASSIGN'" @click="openAssign(row)">
                分配角色
              </el-button>
              <el-button size="small" text type="danger" v-permission="'SYS:USER:DELETE'" @click="handleDelete(row)">
                刪除
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
          :page-sizes="[5, 10, 20, 50]"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增 -->
    <el-dialog v-model="createDialog" title="新增用戶 (POST /api/users)" width="420px">
      <el-form label-width="80px">
        <el-form-item label="帳號">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="createForm.password" type="password" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="createForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">建立</el-button>
      </template>
    </el-dialog>

    <!-- 後台註冊 -->
    <el-dialog v-model="registerDialog" title="後台註冊 (POST /api/users/register)" width="420px">
      <el-form label-width="80px">
        <el-form-item label="帳號">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="registerForm.password" type="password" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="registerForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRegister">註冊</el-button>
      </template>
    </el-dialog>

    <!-- 編輯 -->
    <el-dialog v-model="editDialog" title="編輯用戶 (PUT /api/users)" width="420px">
      <el-form label-width="80px">
        <el-form-item label="帳號">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密碼">
          <el-input v-model="editForm.password" type="password" placeholder="留空則不變" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEdit">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignDialog" title="分配角色 (POST /api/rbac/user-role/assign)" width="420px">
      <p class="muted">需要 sys:rbac:assign 權限</p>
      <template v-if="isAdmin">
        <el-select v-model="selectedRoleIds" multiple filterable placeholder="選擇角色" style="width: 100%">
          <el-option v-for="r in roleOptions" :key="r.id" :label="r.code" :value="r.id" />
        </el-select>
      </template>
      <p v-else class="muted">僅 admin 可分配角色</p>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!isAdmin" @click="handleAssign">儲存</el-button>
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

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
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

.table-card {
  padding: 14px;
}

.notice {
  border: 1px dashed var(--card-border);
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
