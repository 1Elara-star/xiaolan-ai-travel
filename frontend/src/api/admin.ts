import http from '@/api/http'
import type { AdminAttraction, AdminOverview, AdminPlan, AdminUser } from '@/types/admin'

export async function getOverview(): Promise<AdminOverview> {
  const response = await http.get<AdminOverview>('/admin/overview')
  return response.data
}

export async function listUsers(keyword?: string): Promise<AdminUser[]> {
  const response = await http.get<AdminUser[]>('/admin/users', {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
  return response.data
}

export async function updateUserRole(id: number, role: 'USER' | 'ADMIN'): Promise<AdminUser> {
  const response = await http.put<AdminUser>(`/admin/users/${id}/role`, { role })
  return response.data
}

export async function listPlans(keyword?: string): Promise<AdminPlan[]> {
  const response = await http.get<AdminPlan[]>('/admin/plans', {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
  return response.data
}

export async function listAttractions(keyword?: string): Promise<AdminAttraction[]> {
  const response = await http.get<AdminAttraction[]>('/admin/attractions', {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
  return response.data
}
