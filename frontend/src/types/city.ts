export type CitySlug = 'xiamen' | 'chengdu' | 'suzhou'

export interface Attraction {
  id: string
  name: string
  subtitle: string
  category: string
  image: string
  story: string
  popularReason: string
  tags: string[]
  suggestedDuration: string
  photoTip: string
  reminder: string
  city?: string
  address?: string
  longitude?: number | null
  latitude?: number | null
  type?: string
  openTime?: string
  ticketInfo?: string
}

export interface CitySummary {
  slug: string
  name: string
  slogan: string
  description: string
  heroImage: string
  bestSeason: string
  recommendedDays: string
  attractionCount: number
}

export interface FavoriteAttractionResponse {
  favoriteId: number
  favoritedAt: string
  attraction: Attraction
}

export interface CityExploreData {
  slug: string
  name: string
  slogan: string
  description: string
  heroImage: string
  bestSeason: string
  recommendedDays: string
  categories: string[]
  attractions: Attraction[]
}
