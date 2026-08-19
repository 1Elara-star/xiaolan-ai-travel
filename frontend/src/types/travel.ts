export interface TravelPlanRequest {
  title: string
  departureCity: string
  destination: string
  startDate: string
  endDate: string
  peopleCount: number
  companionType?: string
  budget?: number
  tripType?: string
  tripPreferences?: string
  specialRequirements?: string
}

export interface TravelPlan extends TravelPlanRequest {
  id: number
  travelDays: number
  tripStatus: string
  createTime: string | null
  updateTime: string | null
}

export type TravelItemType =
  | 'ATTRACTION'
  | 'FOOD'
  | 'SHOPPING'
  | 'HOTEL'
  | 'EVENT'
  | 'REST'
  | 'OTHER'

export type TravelMode = 'WALKING' | 'DRIVING' | 'BICYCLING' | 'TRANSIT'

export interface TravelPlanItemRequest {
  dayNumber: number
  itemOrder: number
  itemType: TravelItemType
  attractionId?: number | null
  placeName: string
  address?: string | null
  longitude?: number | null
  latitude?: number | null
  startTime?: string | null
  endTime?: string | null
  endDayOffset?: number
  transportMode?: string | null
  distanceFromPrev?: number | null
  travelTimeFromPrev?: number | null
  description?: string | null
}

export interface TravelPlanItem extends TravelPlanItemRequest {
  id: number
  planId: number
  cityCode: string | null
  createTime: string | null
  updateTime: string | null
}

export interface TravelItemLocationResult {
  itemId: number
  poiId: string | null
  poiName: string
  address: string | null
  longitude: number
  latitude: number
  cityCode: string
  source: string
  queriedAt: string | null
}

export interface TravelItemRouteResult {
  fromItemId: number
  toItemId: number
  mode: TravelMode
  distanceMeters: number
  durationSeconds: number | null
  durationMinutes: number
  source: string
  queriedAt: string | null
}

export interface HotelCandidate {
  hotelName: string
  price: string | null
  address: string | null
  latitude: string | null
  longitude: string | null
  imageUrl: string | null
  detailUrl: string | null
  star: string | null
  brandName: string | null
  source: string
}

export interface AiTravelItem {
  placeName: string
  startTime: string | null
  endTime: string | null
  endDayOffset: number
  itemType: string
  description: string | null
}

export interface AiTravelDay {
  dayNumber: number
  theme: string | null
  items: AiTravelItem[]
}

export interface AiTravelPlanResult {
  destination: string
  travelDays: number
  summary: string | null
  days: AiTravelDay[]
}
