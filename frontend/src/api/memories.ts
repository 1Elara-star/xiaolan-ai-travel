import http from '@/api/http'
import type { MemoryType, UserMemory, UserMemoryRequest } from '@/types/memory'

export async function listMemories(filters?: {
  memoryType?: MemoryType
  confirmed?: boolean
}): Promise<UserMemory[]> {
  const response = await http.get<UserMemory[]>('/memories', { params: filters })
  return response.data
}

export async function createMemory(request: UserMemoryRequest): Promise<UserMemory> {
  const response = await http.post<UserMemory>('/memories', request)
  return response.data
}

export async function updateMemory(
  id: number,
  request: UserMemoryRequest,
): Promise<UserMemory> {
  const response = await http.put<UserMemory>(`/memories/${id}`, request)
  return response.data
}

export async function deleteMemory(id: number): Promise<void> {
  await http.delete(`/memories/${id}`)
}
