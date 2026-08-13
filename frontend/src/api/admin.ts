import http from '@/api/http'

export async function verifyAdmin(): Promise<string> {
  const response = await http.get<string>('/admin/test')
  return response.data
}
