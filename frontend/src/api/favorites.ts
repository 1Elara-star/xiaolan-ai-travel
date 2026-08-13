import http from '@/api/http'
import type { FavoriteAttractionResponse } from '@/types/city'

interface FavoriteStatusResponse {
  attractionId: number
  favorited: boolean
}

export async function listFavoriteAttractions(): Promise<FavoriteAttractionResponse[]> {
  const response = await http.get<FavoriteAttractionResponse[]>('/favorites/attractions')
  return response.data
}

export async function addFavoriteAttraction(attractionId: string): Promise<FavoriteStatusResponse> {
  const response = await http.post<FavoriteStatusResponse>(
    `/favorites/attractions/${encodeURIComponent(attractionId)}`,
  )
  return response.data
}

export async function removeFavoriteAttraction(
  attractionId: string,
): Promise<FavoriteStatusResponse> {
  const response = await http.delete<FavoriteStatusResponse>(
    `/favorites/attractions/${encodeURIComponent(attractionId)}`,
  )
  return response.data
}
