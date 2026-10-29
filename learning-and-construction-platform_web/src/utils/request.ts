import axios from 'axios';
import type { AxiosError, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import type { Result } from '../api/types';

// 后端统一返回 Result<T>（code=1 成功），业务失败多数也是 HTTP 200，需要按 code 判断
export class ApiError extends Error {
  code: number;

  constructor(msg: string, code = 0) {
    super(msg);
    this.name = 'ApiError';
    this.code = code;
  }
}

const request = axios.create({
  // 开发环境直连后端（.env 配置），部署到网关/nginx 时可不配该变量走 '/api'
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// ============ Token 过期检测 ============

/**
 * 简易解析 JWT payload（仅 base64 解码中间段，不做签名验证）
 * 用于前端预判 token 是否过期，真正的鉴权仍由后端完成
 */
function isTokenExpired(token: string): boolean {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return false;
    const payload = JSON.parse(atob(parts[1]));
    // exp 是 JWT 标准字段，单位秒
    if (typeof payload.exp !== 'number') return false;
    // 提前 60 秒视为过期，留出缓冲
    return Date.now() >= (payload.exp - 60) * 1000;
  } catch {
    return false;
  }
}

// ============ 防止重复跳转登录页 ============

let isRedirecting = false;

function redirectToLogin() {
  if (isRedirecting) return;
  isRedirecting = true;

  // 清除认证数据
  localStorage.removeItem('token');
  localStorage.removeItem('user_info');

  // 如果当前不在登录页，才跳转
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login';
  }

  // 3 秒后重置标志，允许后续再次触发（防止异常情况下永久锁死）
  setTimeout(() => {
    isRedirecting = false;
  }, 3000);
}

// ============ 请求拦截器 ============

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token) {
      // 预判 token 是否过期，过期则直接跳登录页，不发请求
      if (isTokenExpired(token)) {
        redirectToLogin();
        return Promise.reject(new ApiError('登录已过期，请重新登录', 401));
      }
      config.headers = config.headers || {};
      config.headers['Authorization'] = 'Bearer ' + token;
    }
    return config;
  },
);

// ============ 响应拦截器 ============

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as Result<unknown>;
    if (body && typeof body.code === 'number' && body.code !== 1) {
      return Promise.reject(new ApiError(body.msg || '请求失败', body.code));
    }
    // 已解包出 data，此处仅为满足 axios 拦截器签名；调用方拿到的实际是 T（见 http 包装）
    return body?.data as unknown as AxiosResponse;
  },
  (error: AxiosError<Result<unknown>>) => {
    const status: number | undefined = error.response?.status;
    const url = error.config?.url || '';

    // 登录接口的 401 是账号/密码错误
    if (status === 401 && url.includes('/login')) {
      return Promise.reject(new ApiError('账号或密码错误', 401));
    }

    // 其他接口的 401/403 → token 过期或权限不足，统一跳登录页
    if (status === 401 || status === 403) {
      redirectToLogin();
      const msg =
        status === 403
          ? '没有权限执行此操作'
          : '登录已过期，请重新登录';
      return Promise.reject(new ApiError(msg, status));
    }

    // 网络错误（无 response）
    const msg =
      error.response?.data?.msg ||
      (error.code === 'ECONNABORTED'
        ? '请求超时，请检查网络后重试'
        : '网络错误，请稍后重试');
    return Promise.reject(new ApiError(msg, status ?? 0));
  },
);

// ============ 类型化 HTTP 方法包装 ============

// 响应拦截器已解包出 data，这里对方法做类型化包装，接口层拿到的直接是 T
export const http = {
  get: <T>(url: string, config?: AxiosRequestConfig) => request.get(url, config) as Promise<T>,
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.post(url, data, config) as Promise<T>,
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.put(url, data, config) as Promise<T>,
  delete: <T>(url: string, config?: AxiosRequestConfig) => request.delete(url, config) as Promise<T>,
};

/**
 * 同一 request 实例的另一种类型化包装，用于个别需要覆盖超时的接口（如文件上传 60s）。
 * 行为与 http 完全一致：拦截器已解包，调用方直接拿到 T。
 */
export const requestInstance = {
  get: <T>(url: string, config?: AxiosRequestConfig) => request.get(url, config) as Promise<T>,
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.post(url, data, config) as Promise<T>,
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.put(url, data, config) as Promise<T>,
  delete: <T>(url: string, config?: AxiosRequestConfig) => request.delete(url, config) as Promise<T>,
};

/**
 * 下载文件（如成绩单 Excel）。
 * 走裸 axios 而非上面的实例：二进制响应不能经过「解包 Result」的响应拦截器，
 * 因此这里手动补 baseURL 与 Authorization 头。
 */
export async function downloadFile(url: string, filename: string): Promise<void> {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
  const token = localStorage.getItem('token');
  const res = await axios.get(`${baseURL}${url}`, {
    responseType: 'blob',
    headers: token ? { Authorization: 'Bearer ' + token } : {},
  });

  // 后端出错时会返回 JSON（而非 Excel），此时提示具体原因
  const contentType = String(res.headers['content-type'] || '');
  if (contentType.includes('application/json')) {
    const text = await (res.data as Blob).text();
    try {
      const body = JSON.parse(text) as Result<unknown>;
      throw new ApiError(body.msg || '导出失败', body.code ?? 0);
    } catch (e) {
      if (e instanceof ApiError) throw e;
      throw new ApiError('导出失败', 0);
    }
  }

  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(blobUrl);
}

export default http;
