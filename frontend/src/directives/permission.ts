import type { App, DirectiveBinding } from 'vue';
import { useAuthStore } from '../store/auth';

export function setupPermissionDirective(app: App<Element>) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
      const auth = (useAuthStore as typeof useAuthStore & { getInstance?: () => ReturnType<typeof useAuthStore> }).getInstance?.() || useAuthStore();
      const required = Array.isArray(binding.value)
        ? binding.value.map((v) => (v || '').toUpperCase())
        : (binding.value || '').toUpperCase();
      if (!auth.hasPermission(required)) {
        el.parentNode?.removeChild(el);
      }
    }
  });
}
