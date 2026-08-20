import axios from 'axios'

interface ErrorBody {
  message?: string
}

export function getResponseMessage(error: unknown) {
  if (error instanceof Error && !axios.isAxiosError(error)) return error.message
  if (!axios.isAxiosError(error)) return null
  if (error.code === 'ECONNABORTED') return '行程生成等待超时，请查看 IDEA 后端控制台。'
  if (!error.response) return '无法连接后端，请确认 IDEA 中的后端正在 8081 端口运行。'
  if (typeof error.response?.data === 'string') return error.response.data
  const data = error.response?.data as ErrorBody | undefined
  return typeof data?.message === 'string' ? data.message : null
}
