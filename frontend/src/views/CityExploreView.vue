<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import * as exploreApi from '@/api/explore'
import * as favoritesApi from '@/api/favorites'
import AttractionGrid from '@/components/city/AttractionGrid.vue'
import AttractionStoryPanel from '@/components/city/AttractionStoryPanel.vue'
import CityHeader from '@/components/city/CityHeader.vue'
import FavoriteTray from '@/components/city/FavoriteTray.vue'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { GUEST_ATTRACTION_FAVORITES_KEY, readJsonStorage } from '@/constants/draft'
import { cities, citySlugs } from '@/data/cities'
import { useAuthStore } from '@/stores/auth'
import type { Attraction, CityExploreData, CitySlug } from '@/types/city'
import { getResponseMessage } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const citySlug = computed(() => route.params.city as CitySlug)
const city = ref<CityExploreData>(cities.xiamen)
const activeCategory = ref('全部')
const selectedId = ref(city.value.attractions[0]?.id ?? '')
const guestFavoritesByCity = ref<Record<string, string[]>>(
  readJsonStorage(GUEST_ATTRACTION_FAVORITES_KEY, {}),
)
const serverFavoriteIds = ref<Set<string>>(new Set())
const loading = ref(false)
const pageMessage = ref('')
const favoriteMessage = ref('')
const favoriteBusyIds = ref<Set<string>>(new Set())
let cityRequestId = 0

const visibleAttractions = computed(() => {
  if (activeCategory.value === '全部') {
    return city.value.attractions
  }

  return city.value.attractions.filter((attraction) => attraction.category === activeCategory.value)
})

const selectedAttraction = computed<Attraction | null>(
  () => city.value.attractions.find((attraction) => attraction.id === selectedId.value) ?? null,
)

const favoriteIds = computed(() =>
  authStore.isAuthenticated
    ? city.value.attractions
        .filter((attraction) => serverFavoriteIds.value.has(attraction.id))
        .map((attraction) => attraction.id)
    : (guestFavoritesByCity.value[city.value.slug] ?? []),
)
const favoriteAttractions = computed(() =>
  favoriteIds.value.flatMap((id) =>
    city.value.attractions.filter((attraction) => attraction.id === id),
  ),
)

watch(citySlug, async () => {
  activeCategory.value = '全部'
  await loadCity()
}, { immediate: true })

watch(
  () => authStore.isAuthenticated,
  async (authenticated) => {
    if (authenticated) await loadFavorites()
    else serverFavoriteIds.value = new Set()
  },
  { immediate: true },
)

watch(visibleAttractions, (attractions) => {
  if (attractions.length && !attractions.some((attraction) => attraction.id === selectedId.value)) {
    const firstVisibleAttraction = attractions[0]
    if (firstVisibleAttraction) {
      selectedId.value = firstVisibleAttraction.id
    }
  }
})

function changeCity(slug: CitySlug) {
  void router.push({ name: 'city-explore', params: { city: slug } })
}

function normalizeCity(response: CityExploreData, fallback: CityExploreData): CityExploreData {
  const categories = response.categories?.filter(Boolean) ?? []
  return {
    ...response,
    heroImage: response.heroImage || fallback.heroImage,
    slogan: response.slogan || fallback.slogan,
    description: response.description || fallback.description,
    bestSeason: response.bestSeason || fallback.bestSeason,
    recommendedDays: response.recommendedDays || fallback.recommendedDays,
    categories: ['全部', ...categories.filter((category) => category !== '全部')],
    attractions: (response.attractions ?? []).map((attraction) => ({
      ...attraction,
      subtitle: attraction.subtitle || attraction.type || '旅行地点',
      category: attraction.category || attraction.type || '其他',
      image: attraction.image || response.heroImage || fallback.heroImage,
      story: attraction.story || '这里的故事正在整理中。',
      popularReason: attraction.popularReason || '适合结合自己的旅行节奏慢慢体验。',
      tags: attraction.tags ?? [],
      suggestedDuration: attraction.suggestedDuration || '按实际情况安排',
      photoTip: attraction.photoTip || '留意现场光线和游览秩序。',
      reminder: attraction.reminder || '出发前请再次确认开放时间与现场安排。',
    })),
  }
}

async function loadCity() {
  const requestId = ++cityRequestId
  const fallback = cities[citySlug.value] ?? cities.xiamen
  loading.value = true
  pageMessage.value = ''

  try {
    const response = await exploreApi.getCity(citySlug.value)
    if (requestId !== cityRequestId) return
    city.value = normalizeCity(response, fallback)
  } catch (error) {
    if (requestId !== cityRequestId) return
    city.value = fallback
    pageMessage.value = `${getResponseMessage(error) || '暂时无法读取城市服务'}，当前显示本地城市资料。`
  } finally {
    if (requestId === cityRequestId) loading.value = false
  }

  selectedId.value = city.value.attractions[0]?.id ?? ''
}

function saveGuestFavorites() {
  localStorage.setItem(
    GUEST_ATTRACTION_FAVORITES_KEY,
    JSON.stringify(guestFavoritesByCity.value),
  )
}

async function loadFavorites() {
  favoriteMessage.value = ''
  try {
    const favorites = await favoritesApi.listFavoriteAttractions()
    serverFavoriteIds.value = new Set(favorites.map((item) => String(item.attraction.id)))
    await syncGuestFavorites()
  } catch (error) {
    favoriteMessage.value = getResponseMessage(error) || '收藏暂时无法同步，请稍后重试。'
  }
}

async function syncGuestFavorites() {
  const pendingEntries = Object.entries(guestFavoritesByCity.value)
  let changed = false

  for (const [slug, ids] of pendingEntries) {
    const remaining: string[] = []
    for (const id of ids) {
      if (!/^\d+$/.test(id)) {
        remaining.push(id)
        continue
      }

      try {
        await favoritesApi.addFavoriteAttraction(id)
        serverFavoriteIds.value.add(id)
        changed = true
      } catch {
        remaining.push(id)
      }
    }
    guestFavoritesByCity.value[slug] = remaining
  }

  if (changed) {
    serverFavoriteIds.value = new Set(serverFavoriteIds.value)
    saveGuestFavorites()
    favoriteMessage.value = '登录前选择的地点已同步到账号。'
  }
}

async function toggleFavorite(id: string) {
  if (!authStore.isAuthenticated) {
    const slug = city.value.slug
    const ids = guestFavoritesByCity.value[slug] ?? []
    guestFavoritesByCity.value[slug] = ids.includes(id)
      ? ids.filter((item) => item !== id)
      : [...ids, id]
    saveGuestFavorites()
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  if (favoriteBusyIds.value.has(id)) return
  favoriteBusyIds.value = new Set(favoriteBusyIds.value).add(id)
  favoriteMessage.value = ''
  const wasFavorite = serverFavoriteIds.value.has(id)

  try {
    if (wasFavorite) await favoritesApi.removeFavoriteAttraction(id)
    else await favoritesApi.addFavoriteAttraction(id)

    const next = new Set(serverFavoriteIds.value)
    if (wasFavorite) next.delete(id)
    else next.add(id)
    serverFavoriteIds.value = next
  } catch (error) {
    favoriteMessage.value = getResponseMessage(error) || '收藏操作失败，请稍后重试。'
  } finally {
    const next = new Set(favoriteBusyIds.value)
    next.delete(id)
    favoriteBusyIds.value = next
  }
}

function startPlanning() {
  void router.push({
    name: 'plan-preparation',
    query: { city: city.value.slug, attractions: favoriteIds.value.join(',') },
  })
}

if (!citySlugs.includes(citySlug.value)) {
  void router.replace({ name: 'city-explore', params: { city: 'xiamen' } })
}
</script>

<template>
  <div class="explore-shell">
    <HomeSidebar active="explore" />

    <main>
      <p v-if="loading" class="page-status" role="status">正在读取城市和景点资料…</p>
      <p v-if="pageMessage" class="page-status warning" role="status">{{ pageMessage }}</p>
      <p v-if="favoriteMessage" class="page-status" role="status">{{ favoriteMessage }}</p>

      <CityHeader
        :city="city"
        :active-category="activeCategory"
        @change-city="changeCity"
        @change-category="activeCategory = $event"
      />

      <div class="explore-content">
        <AttractionGrid
          :attractions="visibleAttractions"
          :selected-id="selectedId"
          :favorites="favoriteIds"
          @select="selectedId = $event"
          @toggle-favorite="toggleFavorite"
        />
        <AttractionStoryPanel
          v-if="selectedAttraction"
          :attraction="selectedAttraction"
          :is-favorite="favoriteIds.includes(selectedAttraction.id)"
          @toggle-favorite="toggleFavorite"
        />
      </div>
    </main>

    <FavoriteTray :favorites="favoriteAttractions" @remove="toggleFavorite" @plan="startPlanning" />
  </div>
</template>

<style scoped>
.explore-shell {
  display: grid;
  min-height: 100vh;
  background:
    linear-gradient(rgba(116, 88, 73, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(116, 88, 73, 0.02) 1px, transparent 1px), #faf6f1;
  background-size: 28px 28px;
  grid-template-columns: 178px minmax(0, 1fr);
}

main {
  min-width: 0;
}

.explore-content {
  display: grid;
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 34px;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
}

.explore-content > :last-child {
  margin-top: 34px;
  margin-bottom: 120px;
}

.page-status {
  max-width: 1500px;
  margin: 14px auto 0;
  padding: 10px 34px;
  color: #796b65;
  font-size: 13px;
}

.page-status.warning {
  color: #a45d45;
}

@media (max-width: 1050px) {
  .explore-content {
    grid-template-columns: 1fr;
  }

  .explore-content > :last-child {
    margin-top: -85px;
  }
}

@media (max-width: 900px) {
  .explore-shell {
    display: block;
  }
}

@media (max-width: 640px) {
  .explore-content {
    padding: 0 14px;
  }
}
</style>
