import axios from 'axios'

import { AUTH_TOKEN_KEY, AUTH_UNAUTHORIZED_EVENT, AUTH_USER_KEY } from '@/constants/auth'

/**
 * 前端访问 Spring Boot 后端时统一使用的 Axios 实例。
 *
 * 页面只需要传接口路径，例如 /user/login，
 * 后端地址由环境变量统一管理，避免散落在各个页面中。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const requestUrl = error.config?.url ?? ''
      const isAuthenticationRequest =
        requestUrl.endsWith('/user/login') || requestUrl.endsWith('/user/register')

      if (!isAuthenticationRequest) {
        localStorage.removeItem(AUTH_TOKEN_KEY)
        localStorage.removeItem(AUTH_USER_KEY)
        window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT))
      }
    }

    return Promise.reject(error)
  },
)

export default http
