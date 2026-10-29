import http from '../utils/request';
import type { LoginParams, LoginResult } from './types';

// 登录：POST /login（lp-auth AuthController）
export function login(params: LoginParams): Promise<LoginResult> {
  return http.post<LoginResult>('/login', params);
}
