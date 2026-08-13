import http from '@/api/http'
import type { Attraction, CityExploreData, CitySummary } from '@/types/city'

export async function listCities(keyword?: string): Promise<CitySummary[]> {
  const response = await http.get<CitySummary[]>('/explore/cities', {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
  return response.data
}

export async function getCity(slugOrName: string): Promise<CityExploreData> {
  const response = await http.get<CityExploreData>(`/explore/cities/${encodeURIComponent(slugOrName)}`)
  return response.data
}

export async function listAttractions(params?: {
  city?: string
  keyword?: string
  type?: string
}): Promise<Attraction[]> {
  const response = await http.get<Attraction[]>('/explore/attractions', { params })
  return response.data
}
