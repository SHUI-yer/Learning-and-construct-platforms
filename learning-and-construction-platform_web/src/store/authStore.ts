import { create } from 'zustand';
import type { LoginResult } from '../api/types';
import type { RoleCode } from '../constants/roles';

/** localStorage 中存储用户信息的 key */
const USER_STORAGE_KEY = 'user_info';

/** 从 localStorage 恢复用户信息 */
function loadUser(): LoginResult | null {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as LoginResult) : null;
  } catch {
    return null;
  }
}

/** 将用户信息持久化到 localStorage */
function saveUser(user: LoginResult | null) {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_STORAGE_KEY);
  }
}

interface AuthState {
  /** 登录返回的完整用户信息（含 token） */
  user: LoginResult | null;
  /** 是否已认证（token 存在即 true） */
  isAuthenticated: boolean;
  /** 初始化标志 —— 首次从 localStorage 恢复完成后置为 true */
  initialized: boolean;

  /** 登录：保存完整用户信息 */
  login: (data: LoginResult) => void;
  /** 登出：清除所有认证数据 */
  logout: () => void;
  /** 从 localStorage 恢复状态（应用启动时调用一次） */
  restore: () => void;

  /** 获取当前角色 code */
  getRoleCode: () => RoleCode | null;
  /** 获取当前角色等级 */
  getRoleLevel: () => number | null;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  initialized: false,

  login: (data: LoginResult) => {
    localStorage.setItem('token', data.token);
    saveUser(data);
    set({ user: data, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    saveUser(null);
    set({ user: null, isAuthenticated: false });
  },

  restore: () => {
    const token = localStorage.getItem('token');
    const user = loadUser();
    set({
      user,
      isAuthenticated: !!token && !!user,
      initialized: true,
    });
  },

  getRoleCode: () => (get().user?.roleCode as RoleCode) ?? null,
  getRoleLevel: () => get().user?.roleLevel ?? null,
}));
