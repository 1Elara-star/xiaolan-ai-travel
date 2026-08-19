import * as exploreApi from '@/api/explore'
import * as favoritesApi from '@/api/favorites'
import { GUEST_ATTRACTION_FAVORITES_KEY, readJsonStorage } from '@/constants/draft'
import { cities, citySlugs } from '@/data/cities'
import type { CityExploreData, CitySlug } from '@/types/city'

type GuestFavorites = Record<string, string[]>

export interface FavoriteSyncResult {
  syncedCount: number
  remainingCount: number
}

/**
 * 将游客保存在本机的景点收藏同步到当前登录账号。
 *
 * 旧版演示数据使用诸如 `gulangyu` 的字符串 ID；后端使用数字 ID。
 * 同步时先按景点名称完成一次映射，再调用真实收藏接口。
 */
export async function syncGuestFavoritesToAccount(): Promise<FavoriteSyncResult> {
  const guestFavorites = readJsonStorage<GuestFavorites>(GUEST_ATTRACTION_FAVORITES_KEY, {})
  const remainingFavorites: GuestFavorites = {}
  let syncedCount = 0

  for (const [slug, ids] of Object.entries(guestFavorites)) {
    const uniqueIds = Array.from(new Set(ids))
    if (uniqueIds.length === 0) continue

    const idMap = await buildAttractionIdMap(slug, uniqueIds)
    const remainingIds: string[] = []

    for (const id of uniqueIds) {
      const resolvedId = /^\d+$/.test(id) ? id : idMap.get(id)
      if (!resolvedId) {
        remainingIds.push(id)
        continue
      }

      try {
        await favoritesApi.addFavoriteAttraction(resolvedId)
        syncedCount++
      } catch {
        remainingIds.push(id)
      }
    }

    if (remainingIds.length > 0) remainingFavorites[slug] = remainingIds
  }

  if (Object.keys(remainingFavorites).length > 0) {
    localStorage.setItem(GUEST_ATTRACTION_FAVORITES_KEY, JSON.stringify(remainingFavorites))
  } else {
    localStorage.removeItem(GUEST_ATTRACTION_FAVORITES_KEY)
  }

  return {
    syncedCount,
    remainingCount: Object.values(remainingFavorites).reduce((total, ids) => total + ids.length, 0),
  }
}

async function buildAttractionIdMap(slug: string, ids: string[]) {
  const result = new Map<string, string>()
  if (ids.every((id) => /^\d+$/.test(id))) return result
  if (!citySlugs.includes(slug as CitySlug)) return result

  const citySlug = slug as CitySlug
  const localCity = cities[citySlug]
  let serverCity: CityExploreData

  try {
    serverCity = await exploreApi.getCity(citySlug)
  } catch {
    return result
  }

  const serverIdsByName = new Map(serverCity.attractions.map((item) => [item.name, item.id]))
  for (const localAttraction of localCity.attractions) {
    const serverId = serverIdsByName.get(localAttraction.name)
    if (serverId) result.set(localAttraction.id, serverId)
  }

  return result
}
