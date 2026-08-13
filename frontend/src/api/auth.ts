import http from '@/api/http'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserInfoResponse,
  UserUpdateRequest,
} from '@/types/auth'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await http.post<LoginResponse>('/user/login', request)
  return response.data
}

export async function register(request: RegisterRequest): Promise<string> {
  const response = await http.post<string>('/user/register', request)
  return response.data
}

export async function getCurrentUser(): Promise<UserInfoResponse> {
  const response = await http.get<UserInfoResponse>('/user/me')
  return response.data
}

export async function updateCurrentUser(request: UserUpdateRequest): Promise<UserInfoResponse> {
  const response = await http.put<UserInfoResponse>('/user/me', request)
  return response.data
}
