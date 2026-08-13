import axios from 'axios'

interface ErrorBody {
  message?: string
}

export function getResponseMessage(error: unknown) {
  if (!axios.isAxiosError(error)) return null
  if (typeof error.response?.data === 'string') return error.response.data
  const data = error.response?.data as ErrorBody | undefined
  return typeof data?.message === 'string' ? data.message : null
}
