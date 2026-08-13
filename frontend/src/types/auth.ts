export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  nickname?: string
}

export interface AuthUser {
  userId: number
  username: string
  nickname: string | null
  role: 'USER' | 'ADMIN'
}

export interface LoginResponse extends AuthUser {
  token: string
}

export interface UserInfoResponse {
  id: number
  username: string
  nickname: string | null
  avatar: string | null
  phone: string | null
  email: string | null
  role: 'USER' | 'ADMIN'
}

export interface UserUpdateRequest {
  nickname: string
  avatar: string
  phone: string
  email: string
}
