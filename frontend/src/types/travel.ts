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
  'ATTRACTION' | 'FOOD' | 'SHOPPING' | 'HOTEL' | 'EVENT' | 'REST' | 'OTHER'

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
  straightLineDistanceFromPrev: number | null
  imageUrl: string | null
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
  imageUrl: string | null
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
  priceValue: number | null
  tripMatchScore: number
  profileMatchScore: number
  overallMatchScore: number
  profileUsed: boolean
  recommendationReasons: string[]
}

export type HotelLocationType =
  | 'BUSINESS_AREA'
  | 'TRANSPORT_HUB'
  | 'METRO_STATION'
  | 'SCENIC_AREA'
  | 'LANDMARK'
  | 'ADMINISTRATIVE_AREA'
  | 'CUSTOM'

export interface HotelSearchFilters {
  locationType?: HotelLocationType
  locationKeyword?: string
  minPrice?: number
  maxPrice?: number
}

export interface RecommendedAttraction {
  id: number
  name: string
  city: string
  subtitle: string
  category: string
  image: string
  story: string
  popularReason: string
  tags: string[]
  suggestedDuration: string
  photoTip: string
  reminder: string
  address: string | null
  longitude: number | null
  latitude: number | null
  type: string | null
  openTime: string | null
  ticketInfo: string | null
}

export interface AttractionRecommendation {
  attraction: RecommendedAttraction
  matchPercentage: number
  profileScore: number
  tripScore: number
  favoriteScore: number
  geographyScore: number
  dislikePenalty: number
  nearestPlanDistanceMeters: number | null
  favorite: boolean
  profileUsed: boolean
  recommendationReasons: string[]
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

export interface TravelPlanDraftItem {
  draftItemKey: string
  attractionId: number | null
  sourceType: 'FAVORITE' | 'LOCAL_ATTRACTION' | 'AI_RECOMMENDED'
  dayNumber: number
  itemOrder: number
  itemType: string
  placeName: string
  startTime: string | null
  endTime: string | null
  endDayOffset: number
  description: string | null
  imageUrl: string | null
  storyBackground: string | null
  featureDescription: string | null
  suitableTags: string | null
  suggestDuration: number | null
  openTime: string | null
  ticketInfo: string | null
  poiId: string | null
  matchedPoiName: string | null
  address: string | null
  longitude: number | null
  latitude: number | null
  cityCode: string | null
  transportMode: string | null
  distanceFromPrev: number | null
  travelTimeFromPrev: number | null
  straightLineDistanceFromPrev: number | null
}

export interface TravelPlanDraft {
  planId: number
  destination: string
  travelDays: number
  summary: string | null
  items: TravelPlanDraftItem[]
}

export interface TravelValidationIssue {
  code: string
  severity: 'WARNING' | 'ERROR'
  draftItemKey: string | null
  relatedDraftItemKey: string | null
  message: string
}

export interface TravelDraftSessionResponse {
  draftId: string
  expiresAt: string
  draft: TravelPlanDraft
  validationIssues: TravelValidationIssue[]
  hasErrors: boolean
}

export interface TravelDraftConfirmationResponse {
  planItems: TravelPlanItem[]
  draftSession: TravelDraftSessionResponse | null
}
