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
