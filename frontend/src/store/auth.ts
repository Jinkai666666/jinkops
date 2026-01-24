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
      // 登入前先清掉舊的會話狀態
      this.logout();
      const resp = await loginApi({ username, password });
      this.token = resp.token;
      localStorage.setItem('token', resp.token);
      await this.bootstrap(true);
    },
    async bootstrap(forceVerify = false) {
      if (!this.token) {
        return;
      }
      if (!this.username || forceVerify) {
        const userName = await verifyApi(this.token);
        this.username = userName;
        localStorage.setItem('username', userName);
      }
      let detail: User | null = null;
      try {
        detail = await getUserByUsername(this.username, { __silent: true, headers: { 'X-Silent': '1' } });
      } catch (e: any) {
        // 允許權限不足（403）繼續登入，但不附帶任何權限
        const code = e?.code ?? e?.status;
        if (code !== 403) {
          throw e;
        }
      }
      const perms = new Set<string>();
      detail?.roles?.forEach((role) => {
        const roleCode = (role.code || '').toUpperCase();
        perms.add(`ROLE_${roleCode}`);
        role.permissions?.forEach((p) => perms.add((p.code || '').toUpperCase()));
      });
      this.user = detail || ({ username: this.username, roles: [] } as User);
      // 後端權限代碼大小寫不定，統一轉大寫以符合前端校驗
      this.permissions = Array.from(perms);
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
  // 在非 setup 檔（例如 axios 拦截器）保留單例，避免 pinia 未安裝前重複建 store
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const globalAny = globalThis as any;
  if (!globalAny.__auth_store_instance) {
    globalAny.__auth_store_instance = useAuthStore();
  }
  return globalAny.__auth_store_instance as AuthStore;
};
