export type MemoryType = 'PREFERENCE' | 'DISLIKE' | 'EXPERIENCE' | 'REMINDER'

export interface UserMemoryRequest {
  memoryType: MemoryType
  memoryContent: string
  userConfirmed: boolean
}

export interface UserMemory extends UserMemoryRequest {
  id: number
  createTime: string | null
  updateTime: string | null
}
