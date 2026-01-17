<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { listPermissions, createPermission, deletePermission } from '../api/permissions';
import type { Permission } from '../api/types';
import { useAuthStore } from '../store/auth';

type TreeNode = {
  id?: number | string;
  label: string;
  code?: string;
  children?: TreeNode[];
  isLeaf?: boolean;
};

const loading = ref(false);
const permissions = ref<Permission[]>([]);
const treeData = ref<TreeNode[]>([]);
const auth = useAuthStore();
const isAdmin = computed(() => auth.permissions.includes('ROLE_ADMIN'));
const canList = computed(() => isAdmin.value || auth.hasPermission('SYS:PERM:LIST'));
const canCreate = computed(() => isAdmin.value || auth.hasPermission('SYS:PERM:CREATE'));
const canDelete = computed(() => isAdmin.value || auth.hasPermission('SYS:PERM:DELETE'));

const addForm = reactive({
  code: ''
});
const adding = ref(false);
const search = ref('');
let searchTimer: number | undefined;

const fetchPermissions = async () => {
  if (!canList.value) {
    ElMessage.error('无权限');
    return;
  }
  loading.value = true;
  try {
    permissions.value = await listPermissions();
    treeData.value = buildTree(permissions.value);
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  } finally {
    loading.value = false;
  }
};

const buildTree = (perms: Permission[]): TreeNode[] => {
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

const filteredTree = computed(() => {
  if (!search.value.trim()) {
    return treeData.value;
  }
  const keyword = search.value.toLowerCase();
  const filterNode = (nodes: TreeNode[]): TreeNode[] => {
    const res: TreeNode[] = [];
    nodes.forEach((n) => {
      const match = (n.code || '').toLowerCase().includes(keyword) || n.label.toLowerCase().includes(keyword);
      const children = n.children ? filterNode(n.children) : [];
      if (match || children.length) {
        res.push({ ...n, children });
      }
    });
    return res;
  };
  return filterNode(treeData.value);
});

const handleCreate = async () => {
  if (!addForm.code.trim()) {
    ElMessage.warning('请输入权限编码，如 sys:user:list');
    return;
  }
  if (!canCreate.value) {
    ElMessage.error('无权限');
    return;
  }
  adding.value = true;
  try {
    await createPermission({ code: addForm.code.trim() });
    ElMessage.success('新增成功');
    addForm.code = '';
    fetchPermissions();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  } finally {
    adding.value = false;
  }
};

const handleDelete = async (node: TreeNode) => {
  if (!node.id || !node.isLeaf) return;
  if (!canDelete.value) {
    ElMessage.error('无权限');
    return;
  }
  try {
    await deletePermission(Number(node.id));
    ElMessage.success('已删除');
    fetchPermissions();
  } catch (e: any) {
    if (e?.status === 403) ElMessage.error('无权限');
  }
};

const onSearchInput = (val: string) => {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(() => {
    search.value = val;
  }, 300);
};

watch(
  () => permissions.value,
  () => {
    treeData.value = buildTree(permissions.value);
  }
);

onMounted(() => {
  fetchPermissions();
});
</script>

<template>
  <div class="perm-view page">
    <div class="header glass-card">
      <div>
        <p class="eyebrow">权限管理</p>
        <h2>权限树（仅管理员可见/修改）</h2>
        <p class="muted">支持新增、删除权限。按钮会根据权限自动禁用或隐藏。</p>
      </div>
      <div class="actions">
        <el-input
          placeholder="搜索权限编码"
          clearable
          @input="onSearchInput"
          style="width: 240px"
          :disabled="loading || !canList"
        />
        <el-space>
          <el-input
            v-model="addForm.code"
            placeholder="如 sys:user:list"
            style="width: 220px"
            :disabled="loading || !canCreate"
          />
          <el-button
            type="primary"
            :loading="adding"
            :disabled="!canCreate"
            v-permission="'sys:perm:create'"
            @click="handleCreate"
          >
            新增
          </el-button>
        </el-space>
      </div>
    </div>

    <div class="glass-card tree-card" v-loading="loading">
      <el-tree
        :data="filteredTree"
        node-key="id"
        :props="{ children: 'children', label: 'label' }"
        default-expand-all
        highlight-current
        class="perm-tree"
        :disabled="!canList"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span>{{ data.code || data.label }}</span>
            <el-button
              v-if="data.isLeaf"
              size="small"
              text
              type="danger"
              :disabled="!canDelete"
              v-permission="'sys:perm:delete'"
              @click.stop="handleDelete(data)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<style scoped>
.perm-view {
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

.tree-card {
  padding: 14px;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.perm-tree {
  --el-tree-node-hover-bg-color: rgba(255, 255, 255, 0.03);
}
</style>
