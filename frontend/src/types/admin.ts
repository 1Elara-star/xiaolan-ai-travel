export interface AdminOverview {
  userCount: number
  adminCount: number
  planCount: number
  attractionCount: number
  favoriteCount: number
  memoryCount: number
}

export interface AdminUser {
  id: number
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  role: 'USER' | 'ADMIN'
  createTime: string | null
  updateTime: string | null
}

export interface AdminPlan {
  id: number
  title: string
  userId: number
  username: string
  destination: string
  startDate: string
  endDate: string
  travelDays: number
  tripStatus: string
  createTime: string | null
}

export interface AdminAttraction {
  id: number
  name: string
  city: string
  type: string | null
  address: string | null
  createTime: string | null
}
