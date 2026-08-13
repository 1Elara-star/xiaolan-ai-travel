export const HOME_IDEA_KEY = 'xiaolan-home-travel-idea'
export const HOME_FAVORITES_KEY = 'xiaolan-home-favorites'
export const GUEST_ATTRACTION_FAVORITES_KEY = 'xiaolan-guest-attraction-favorites'
export const PLAN_DRAFT_KEY = 'xiaolan-plan-draft'

export function readJsonStorage<T>(key: string, fallback: T): T {
  const value = localStorage.getItem(key)
  if (!value) return fallback

  try {
    return JSON.parse(value) as T
  } catch {
    localStorage.removeItem(key)
    return fallback
  }
}
