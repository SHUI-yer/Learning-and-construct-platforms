import { useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import type { LoginResult } from '../api/types';
import type { RoleCode } from '../constants/roles';

/**
 * 认证 hook —— 基于 Zustand store，替代原先直接操作 localStorage 的版本
 *
 * 用法与之前保持一致：<Button onClick={logout}>退出</Button>
 * 新增：user / roleCode / roleLevel / hasRole / isAdmin 等便捷属性
 */
export function useAuth() {
  const {
    user,
    isAuthenticated,
    initialized,
    login: storeLogin,
    logout: storeLogout,
    restore,
    getRoleCode,
    getRoleLevel,
  } = useAuthStore();

  /** 登录 —— 保存后端返回的完整用户信息 */
  const login = useCallback(
    (data: LoginResult) => {
      storeLogin(data);
    },
    [storeLogin],
  );

  /** 登出 —— 清除 token 和用户信息 */
  const logout = useCallback(() => {
    storeLogout();
  }, [storeLogout]);

  /** 初始化：从 localStorage 恢复认证状态 */
  const initAuth = useCallback(() => {
    restore();
  }, [restore]);

  /** 判断当前角色是否匹配 */
  const hasRole = useCallback(
    (...codes: RoleCode[]) => {
      const current = getRoleCode();
      return current !== null && codes.includes(current);
    },
    [getRoleCode],
  );

  /** 是否为管理员（super_admin 或 dept_admin） */
  const admin =
    user?.roleCode === 'super_admin' || user?.roleCode === 'dept_admin';

  return {
    // 状态
    user,
    isAuthenticated,
    initialized,

    // 用户属性快捷访问
    userId: user?.userId ?? null,
    username: user?.username ?? '',
    realName: user?.realName ?? '',
    roleCode: getRoleCode(),
    roleLevel: getRoleLevel(),

    // 方法
    login,
    logout,
    initAuth,
    hasRole,
    isAdmin: admin,
  };
}
