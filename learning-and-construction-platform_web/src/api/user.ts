import type { LoginResult } from './types';

/**
 * 获取当前登录用户信息
 * 后端暂未实现此接口，前端暂用 localStorage 缓存的登录数据
 * 待后端提供 GET /user/info 后取消注释即可
 */
// export function getUserInfo(): Promise<LoginResult> {
//   return http.get<LoginResult>('/user/info');
// }

/**
 * 修改密码
 * 待后端提供 PUT /user/password 后启用
 */
// export function changePassword(params: { oldPassword: string; newPassword: string }): Promise<void> {
//   return http.put<void>('/user/password', params);
// }

/**
 * 更新用户资料
 * 待后端提供 PUT /user/profile 后启用
 */
// export function updateProfile(params: { realName?: string; email?: string; phone?: string; avatarUrl?: string }): Promise<void> {
//   return http.put<void>('/user/profile', params);
// }

// 暂时导出空对象以避免 unused 警告
export const userApiPlaceholder = true;

// 重新导出类型供外部使用
export type { LoginResult };
