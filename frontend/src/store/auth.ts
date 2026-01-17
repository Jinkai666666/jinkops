import { defineStore } from 'pinia';
import { login as loginApi, verify as verifyApi } from '../api/auth';
import { getUserByUsername } from '../api/users';
import type { User } from '../api/types';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    user: null as User | null,
    permissions: [] as string[]
  }),
  actions: {
    async login(username: string, password: string) {
      const resp = await loginApi({ username, password });
      this.token = resp.token;
      localStorage.setItem('token', resp.token);
      await this.bootstrap();
    },
    async bootstrap() {
      if (!this.token) {
        return;
      }
      if (!this.username) {
        const userName = await verifyApi(this.token);
        this.username = userName;
        localStorage.setItem('username', userName);
      }
      if (this.username) {
        const detail = await getUserByUsername(this.username);
        this.user = detail;
        const perms = new Set<string>();
        detail.roles?.forEach((role) => {
          const roleCode = (role.code || '').toUpperCase();
          perms.add(`ROLE_${roleCode}`);
          role.permissions?.forEach((p) => perms.add((p.code || '').toUpperCase()));
        });
        // 后端权限码返回大小写不定，统一大写便于比较
        this.permissions = Array.from(perms);
      }
    },
    logout() {
      this.token = '';
      this.username = '';
      this.user = null;
      this.permissions = [];
      localStorage.removeItem('token');
      localStorage.removeItem('username');
    },
    hasPermission(codes: string | string[]) {
      const isAdmin =
        this.user?.roles?.some((r) => (r.code || '').toUpperCase().includes('ADMIN')) ||
        this.permissions.includes('ROLE_ADMIN');
      if (isAdmin) return true;
      if (!codes) return true;
      if (!this.permissions.length) return false;
      if (Array.isArray(codes)) {
        return codes.some((c) => this.permissions.includes((c || '').toUpperCase()));
      }
      return this.permissions.includes((codes || '').toUpperCase());
    }
  }
});

type AuthStore = ReturnType<typeof useAuthStore>;

(useAuthStore as typeof useAuthStore & { getInstance?: () => AuthStore }).getInstance = () => {
  // keep a single instance for usage inside non-setup files (e.g., axios interceptors)
  // to avoid creating stores before pinia is installed.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const globalAny = globalThis as any;
  if (!globalAny.__auth_store_instance) {
    globalAny.__auth_store_instance = useAuthStore();
  }
  return globalAny.__auth_store_instance as AuthStore;
};
