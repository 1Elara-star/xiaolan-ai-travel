import http from '@/api/http'
import type {
  AiTravelPlanResult,
  HotelCandidate,
  TravelItemLocationResult,
  TravelItemRouteResult,
  TravelMode,
  TravelPlan,
  TravelPlanItem,
  TravelPlanItemRequest,
  TravelPlanRequest,
} from '@/types/travel'

export async function createPlan(request: TravelPlanRequest): Promise<TravelPlan> {
  const response = await http.post<TravelPlan>('/travel/plan', request)
  return response.data
}

export async function listMyPlans(): Promise<TravelPlan[]> {
  const response = await http.get<TravelPlan[]>('/travel/plan/my')
  return response.data
}

export async function deletePlan(id: number): Promise<void> {
  await http.delete(`/travel/plan/${id}`)
}

export async function getPlan(id: number): Promise<TravelPlan> {
  const response = await http.get<TravelPlan>(`/travel/plan/${id}`)
  return response.data
}

export async function updatePlan(id: number, request: TravelPlanRequest): Promise<TravelPlan> {
  const response = await http.put<TravelPlan>(`/travel/plan/${id}`, request)
  return response.data
}

export async function listPlanItems(planId: number): Promise<TravelPlanItem[]> {
  const response = await http.get<TravelPlanItem[]>(`/travel/plan/${planId}/items`)
  return response.data
}

export async function createPlanItem(
  planId: number,
  request: TravelPlanItemRequest,
): Promise<TravelPlanItem> {
  const response = await http.post<TravelPlanItem>(`/travel/plan/${planId}/items`, request)
  return response.data
}

export async function updatePlanItem(
  planId: number,
  itemId: number,
  request: TravelPlanItemRequest,
): Promise<TravelPlanItem> {
  const response = await http.put<TravelPlanItem>(
    `/travel/plan/${planId}/items/${itemId}`,
    request,
  )
  return response.data
}

export async function deletePlanItem(planId: number, itemId: number): Promise<void> {
  await http.delete(`/travel/plan/${planId}/items/${itemId}`)
}

export async function generatePlanWithAi(planId: number): Promise<AiTravelPlanResult> {
  const response = await http.post<AiTravelPlanResult>(`/travel/plan/${planId}/ai/generate`)
  return response.data
}

export async function resolveItemLocation(
  planId: number,
  itemId: number,
  refresh = false,
): Promise<TravelItemLocationResult> {
  const response = await http.post<TravelItemLocationResult>(
    `/travel/plan/${planId}/items/${itemId}/map/location`,
    null,
    { params: { refresh } },
  )
  return response.data
}

export async function calculateItemRoute(
  planId: number,
  itemId: number,
  mode: TravelMode,
  refresh = false,
): Promise<TravelItemRouteResult> {
  const response = await http.post<TravelItemRouteResult>(
    `/travel/plan/${planId}/items/${itemId}/map/route-from-previous`,
    null,
    { params: { mode, refresh } },
  )
  return response.data
}

export async function listHotelCandidates(planId: number): Promise<HotelCandidate[]> {
  const response = await http.get<HotelCandidate[]>(`/travel/plan/${planId}/hotels`)
  return response.data
}
