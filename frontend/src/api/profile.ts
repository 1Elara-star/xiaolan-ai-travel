import http from '@/api/http'
import type { UserProfile } from '@/types/profile'

export async function getProfile(): Promise<UserProfile> {
  const response = await http.get<UserProfile>('/user/profile')
  return response.data
}

export async function saveProfile(profile: UserProfile): Promise<UserProfile> {
  const response = await http.put<UserProfile>('/user/profile', profile)
  return response.data
}
