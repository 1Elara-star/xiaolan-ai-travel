import type { CitySlug } from '@/types/city'

export interface InspirationCard {
  slug: CitySlug
  city: string
  duration: string
  theme: string
  description: string
  favorites: string
  image: string
}
