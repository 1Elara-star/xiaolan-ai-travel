import http from '@/api/http'
import type { TravelPlan, TravelPlanRequest } from '@/types/travel'

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
