<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import * as travelApi from '@/api/travel'
import AttractionRecommendationList from '@/components/travel/AttractionRecommendationList.vue'
import HotelCandidateList from '@/components/travel/HotelCandidateList.vue'
import PlanItemForm from '@/components/travel/PlanItemForm.vue'
import PlanOverviewCard from '@/components/travel/PlanOverviewCard.vue'
import PlanTimeline from '@/components/travel/PlanTimeline.vue'
import TravelDraftPreview from '@/components/travel/TravelDraftPreview.vue'
import { getHotelLocationOptions } from '@/data/hotelBusinessAreas'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import type {
  AttractionRecommendation,
  HotelCandidate,
  HotelSearchFilters,
  TravelPlanDraft,
  TravelMode,
  TravelItemLocationResult,
  TravelPlan,
  TravelPlanItem,
  TravelPlanItemRequest,
  TravelPlanRequest,
} from '@/types/travel'
import { getResponseMessage } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.id))
const startInEditMode = computed(() => route.query.edit === '1')
const plan = ref<TravelPlan | null>(null)
const items = ref<TravelPlanItem[]>([])
const hotels = ref<HotelCandidate[]>([])
const attractionRecommendations = ref<AttractionRecommendation[]>([])
const recommendationsLoaded = ref(false)
const loadingRecommendations = ref(false)
const addingAttractionId = ref<number | null>(null)
const hotelsLoaded = ref(false)
const loading = ref(true)
const savingPlan = ref(false)
const savingItem = ref(false)
const generating = ref(false)
const enrichingDraft = ref(false)
const adoptingDraft = ref(false)
const loadingHotels = ref(false)
const parsingHotelPreference = ref(false)
const parsedHotelFilters = ref<HotelSearchFilters | null>(null)
const busyAction = ref('')
const editingItem = ref<TravelPlanItem | null>(null)
const showItemForm = ref(false)
const itemEditor = ref<HTMLElement | null>(null)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const locationResults = ref<Record<number, TravelItemLocationResult>>({})
const generatedDraft = ref<TravelPlanDraft | null>(null)
const draftExpiresAt = ref<string | null>(null)
const draftId = ref<string | null>(null)
const savedItinerary = ref<HTMLElement | null>(null)
const selectedHotelName = computed(
  () =>
    items.value.find((item) => item.itemType === 'HOTEL' && item.placeName !== '待推荐酒店')
      ?.placeName ?? null,
)
const hotelLocationOptions = computed(() => getHotelLocationOptions(plan.value?.destination))

onMounted(loadPage)

async function loadPage() {
  if (!Number.isInteger(planId.value) || planId.value <= 0) {
    await router.replace({ name: 'plans' })
    return
  }

  loading.value = true
  setMessage('')
  try {
    const [savedPlan, savedItems] = await Promise.all([
      travelApi.getPlan(planId.value),
      travelApi.listPlanItems(planId.value),
    ])
    plan.value = savedPlan
    items.value = savedItems
    await loadAttractionRecommendations()
    const notice = sessionStorage.getItem('xiaolan-plan-notice')
    if (notice) {
      sessionStorage.removeItem('xiaolan-plan-notice')
      setMessage(notice, 'error')
    }
  } catch (error) {
    setMessage(getResponseMessage(error) || '暂时无法读取这份行程。', 'error')
  } finally {
    loading.value = false
  }
}

async function loadAttractionRecommendations() {
  if (!Number.isInteger(planId.value) || planId.value <= 0) return
  loadingRecommendations.value = true
  try {
    attractionRecommendations.value = await travelApi.listAttractionRecommendations(planId.value)
    recommendationsLoaded.value = true
  } catch (error) {
    setMessage(getResponseMessage(error) || '个性化景点推荐读取失败。', 'error')
  } finally {
    loadingRecommendations.value = false
  }
}

async function addRecommendedAttraction(
  recommendation: AttractionRecommendation,
  dayNumber: number,
) {
  const attraction = recommendation.attraction
  const dayItems = items.value.filter((item) => item.dayNumber === dayNumber)
  const itemOrder = Math.max(0, ...dayItems.map((item) => item.itemOrder)) + 1
  addingAttractionId.value = attraction.id
  try {
    await travelApi.createPlanItem(planId.value, {
      dayNumber,
      itemOrder,
      itemType: 'ATTRACTION',
      attractionId: attraction.id,
      placeName: attraction.name,
      address: attraction.address,
      longitude: attraction.longitude,
      latitude: attraction.latitude,
      startTime: null,
      endTime: null,
      endDayOffset: 0,
      transportMode: null,
      distanceFromPrev: null,
      travelTimeFromPrev: null,
      description: attraction.popularReason || attraction.subtitle || null,
    })
    items.value = await travelApi.listPlanItems(planId.value)
    await loadAttractionRecommendations()
    setMessage(`已将“${attraction.name}”加入 Day ${dayNumber}，时间可以在详细行程中继续编辑。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '推荐景点加入行程失败。', 'error')
  } finally {
    addingAttractionId.value = null
  }
}

function setMessage(value: string, type: 'success' | 'error' = 'success') {
  message.value = value
  messageType.value = type
}

async function openNewItem() {
  editingItem.value = null
  showItemForm.value = true
  await focusItemEditor()
}

async function openEditItem(item: TravelPlanItem) {
  editingItem.value = item
  showItemForm.value = true
  await focusItemEditor()
}

async function focusItemEditor() {
  await nextTick()
  itemEditor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function savePlan(request: TravelPlanRequest) {
  savingPlan.value = true
  try {
    plan.value = await travelApi.updatePlan(planId.value, request)
    setMessage('旅行需求已更新。')
    if (route.query.edit === '1') {
      await router.replace({ name: 'plan-detail', params: { id: planId.value } })
    }
  } catch (error) {
    setMessage(getResponseMessage(error) || '旅行需求保存失败。', 'error')
  } finally {
    savingPlan.value = false
  }
}

async function saveItem(request: TravelPlanItemRequest) {
  savingItem.value = true
  try {
    if (editingItem.value) {
      await travelApi.updatePlanItem(planId.value, editingItem.value.id, request)
      setMessage('行程节点已更新。')
    } else {
      await travelApi.createPlanItem(planId.value, request)
      setMessage('行程节点已添加。')
    }
    items.value = await travelApi.listPlanItems(planId.value)
    showItemForm.value = false
    editingItem.value = null
  } catch (error) {
    setMessage(getResponseMessage(error) || '行程节点保存失败。', 'error')
  } finally {
    savingItem.value = false
  }
}

async function removeItem(item: TravelPlanItem) {
  if (!window.confirm(`确定删除“${item.placeName}”吗？`)) return
  try {
    await travelApi.deletePlanItem(planId.value, item.id)
    items.value = items.value.filter((candidate) => candidate.id !== item.id)
    setMessage('行程节点已删除。')
  } catch (error) {
    setMessage(getResponseMessage(error) || '删除行程节点失败。', 'error')
  }
}

async function generateWithAi(additionalRequirements?: string) {
  generating.value = true
  setMessage('')
  try {
    const result = await travelApi.generatePlanWithAi(planId.value, additionalRequirements)
    if (!result.draft) {
      throw new Error('后端仍在返回旧版行程格式，请在 IDEA 中停止后重新启动后端。')
    }
    generatedDraft.value = result.draft
    draftId.value = result.draftId
    draftExpiresAt.value = result.expiresAt
    setMessage(
      result.draft.summary ||
        `已生成 ${result.draft.travelDays} 天、${result.draft.items.length} 个节点的候选行程。`,
    )
  } catch (error) {
    setMessage(getResponseMessage(error) || 'DeepSeek 行程生成失败。', 'error')
  } finally {
    generating.value = false
  }
}

async function enrichGeneratedDraft() {
  if (!draftId.value || !generatedDraft.value) return

  const uniqueMissingPlaces = new Set(
    generatedDraft.value.items
      .filter(
        (item) =>
          (item.itemType === 'ATTRACTION' || item.itemType === 'EVENT') &&
          (item.longitude == null || item.latitude == null),
      )
      .map((item) => item.placeName.trim().toLowerCase()),
  )
  const prompt = uniqueMissingPlaces.size
    ? `将最多查询 ${uniqueMissingPlaces.size} 个缺少坐标的具体地点，并计算本地直线距离。是否继续？`
    : '这些地点已有本地坐标，本次只在后端计算直线距离，不会请求高德。是否继续？'
  if (!window.confirm(prompt)) return

  enrichingDraft.value = true
  try {
    const result = await travelApi.enrichDraftMap(draftId.value)
    generatedDraft.value = result.draft
    draftExpiresAt.value = result.expiresAt
    setMessage('候选地点和直线距离已更新。你仍然可以只选择满意的节点。')
  } catch (error) {
    setMessage(
      getResponseMessage(error) || '地图补全失败，但候选行程仍然保留，可以继续选择。',
      'error',
    )
  } finally {
    enrichingDraft.value = false
  }
}

async function adoptGeneratedItems(selectedKeys: string[]) {
  if (!draftId.value || selectedKeys.length === 0) return
  if (
    !window.confirm(
      `确定将选中的 ${selectedKeys.length} 个候选节点加入详细行程吗？已有节点不会被删除。`,
    )
  ) {
    return
  }

  adoptingDraft.value = true
  try {
    const existingIds = new Set(items.value.map((item) => item.id))
    const selectedDraftItems = (generatedDraft.value?.items ?? [])
      .filter((item) => selectedKeys.includes(item.draftItemKey))
      .sort((left, right) => left.dayNumber - right.dayNumber || left.itemOrder - right.itemOrder)
    const result = await travelApi.adoptDraftItems(draftId.value, selectedKeys)
    const newlyAddedItems = result.planItems
      .filter((item) => !existingIds.has(item.id))
      .sort((left, right) => left.dayNumber - right.dayNumber || left.itemOrder - right.itemOrder)

    const transferredLocations = { ...locationResults.value }
    selectedDraftItems.forEach((draftItem, index) => {
      const savedItem = newlyAddedItems[index]
      if (
        !savedItem ||
        !draftItem.imageUrl ||
        draftItem.longitude == null ||
        draftItem.latitude == null
      ) {
        return
      }
      transferredLocations[savedItem.id] = {
        itemId: savedItem.id,
        poiId: draftItem.poiId,
        poiName: draftItem.matchedPoiName || draftItem.placeName,
        address: draftItem.address,
        longitude: draftItem.longitude,
        latitude: draftItem.latitude,
        cityCode: draftItem.cityCode || '',
        imageUrl: draftItem.imageUrl,
        source: 'DRAFT_SESSION',
        queriedAt: null,
      }
    })
    locationResults.value = transferredLocations
    items.value = result.planItems

    if (result.draftSession) {
      draftId.value = result.draftSession.draftId
      draftExpiresAt.value = result.draftSession.expiresAt
      generatedDraft.value = result.draftSession.draft
    } else {
      draftId.value = null
      draftExpiresAt.value = null
      generatedDraft.value = null
    }

    setMessage(`已将 ${selectedKeys.length} 个候选节点加入详细行程，其他候选和已有节点没有被改动。`)
    await nextTick()
    savedItinerary.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch (error) {
    setMessage(getResponseMessage(error) || '候选节点加入详细行程失败。', 'error')
  } finally {
    adoptingDraft.value = false
  }
}

async function resolveLocation(item: TravelPlanItem) {
  const alreadyMatched = item.longitude != null && item.latitude != null
  const action = alreadyMatched ? '重新匹配' : '匹配'
  if (!window.confirm(`将调用一次高德，为“${item.placeName}”${action}真实地点，是否继续？`)) return
  busyAction.value = `location-${item.id}`
  try {
    const result = await travelApi.resolveItemLocation(planId.value, item.id, alreadyMatched)
    locationResults.value = { ...locationResults.value, [item.id]: result }
    items.value = await travelApi.listPlanItems(planId.value)
    setMessage(`“${item.placeName}”已匹配到真实地点：${result.poiName}`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '地点匹配失败。', 'error')
  } finally {
    busyAction.value = ''
  }
}

async function calculateRoute(item: TravelPlanItem, mode: TravelMode) {
  if (!window.confirm('将调用一次高德路线服务，是否继续？')) return
  busyAction.value = `route-${item.id}`
  try {
    const result = await travelApi.calculateItemRoute(planId.value, item.id, mode)
    items.value = await travelApi.listPlanItems(planId.value)
    setMessage(`路线已计算：${result.distanceMeters} 米，约 ${result.durationMinutes} 分钟。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '路线计算失败。', 'error')
  } finally {
    busyAction.value = ''
  }
}

async function loadHotels(filters: HotelSearchFilters) {
  loadingHotels.value = true
  try {
    hotels.value = await travelApi.listHotelCandidates(planId.value, filters)
    hotelsLoaded.value = true
    setMessage(`已获得 ${hotels.value.length} 个酒店候选。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '酒店候选查询失败。', 'error')
  } finally {
    loadingHotels.value = false
  }
}

async function parseHotelPreference(preference: string) {
  parsingHotelPreference.value = true
  try {
    const filters = await travelApi.parseHotelPreference(planId.value, preference)
    parsedHotelFilters.value = filters
    await loadHotels(filters)
  } catch (error) {
    setMessage(getResponseMessage(error) || '住宿需求整理失败，请换一种说法再试。', 'error')
  } finally {
    parsingHotelPreference.value = false
  }
}

async function selectHotel(hotel: HotelCandidate) {
  const hotelItem =
    items.value.find((item) => item.itemType === 'HOTEL' && item.placeName === '待推荐酒店') ??
    items.value.find((item) => item.itemType === 'HOTEL')

  if (!hotelItem) {
    setMessage('当前行程没有酒店节点，请先在详细行程中添加一个酒店节点。', 'error')
    return
  }

  if (
    hotelItem.placeName !== '待推荐酒店' &&
    !window.confirm(`确定把“${hotelItem.placeName}”更换为“${hotel.hotelName}”吗？`)
  ) {
    return
  }

  savingItem.value = true
  try {
    await travelApi.updatePlanItem(planId.value, hotelItem.id, {
      dayNumber: hotelItem.dayNumber,
      itemOrder: hotelItem.itemOrder,
      itemType: 'HOTEL',
      attractionId: null,
      placeName: hotel.hotelName,
      address: hotel.address,
      longitude: parseCoordinate(hotel.longitude, -180, 180),
      latitude: parseCoordinate(hotel.latitude, -90, 90),
      startTime: hotelItem.startTime,
      endTime: hotelItem.endTime,
      endDayOffset: hotelItem.endDayOffset,
      transportMode: null,
      distanceFromPrev: null,
      travelTimeFromPrev: null,
      description: hotelItem.description,
    })
    items.value = await travelApi.listPlanItems(planId.value)
    setMessage(`已选择“${hotel.hotelName}”作为住宿。请先匹配上一站位置，再选择交通方式计算路线。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '酒店保存到行程失败。', 'error')
  } finally {
    savingItem.value = false
  }
}

function parseCoordinate(value: string | null, min: number, max: number) {
  if (!value) return null
  const coordinate = Number(value)
  return Number.isFinite(coordinate) && coordinate >= min && coordinate <= max ? coordinate : null
}
</script>

<template>
  <div class="page-shell">
    <HomeSidebar active="plans" />
    <main>
      <RouterLink class="back" :to="{ name: 'plans' }">← 返回我的旅行</RouterLink>

      <p v-if="message" class="message" :class="messageType" role="status">{{ message }}</p>
      <div v-if="loading" class="loading">正在读取旅行计划…</div>

      <template v-else-if="plan">
        <PlanOverviewCard
          :plan="plan"
          :saving="savingPlan"
          :generating="generating"
          :initially-editing="startInEditMode"
          @save="savePlan"
          @generate="generateWithAi"
        />

        <AttractionRecommendationList
          :recommendations="attractionRecommendations"
          :travel-days="plan.travelDays"
          :loading="loadingRecommendations"
          :loaded="recommendationsLoaded"
          :adding-attraction-id="addingAttractionId"
          @reload="loadAttractionRecommendations"
          @add="addRecommendedAttraction"
        />

        <TravelDraftPreview
          v-if="generatedDraft"
          :draft="generatedDraft"
          :expires-at="draftExpiresAt"
          :enriching="enrichingDraft"
          :adopting="adoptingDraft"
          :regenerating="generating"
          @enrich="enrichGeneratedDraft"
          @adopt="adoptGeneratedItems"
          @regenerate="generateWithAi"
        />

        <div v-if="showItemForm" ref="itemEditor" class="item-editor-anchor">
          <PlanItemForm
            :plan-days="plan.travelDays"
            :item="editingItem"
            :saving="savingItem"
            @submit="saveItem"
            @cancel="showItemForm = false"
          />
        </div>

        <div ref="savedItinerary" class="saved-itinerary">
          <PlanTimeline
            :items="items"
            :travel-days="plan.travelDays"
            :busy-action="busyAction"
            :location-results="locationResults"
            @add="openNewItem"
            @edit="openEditItem"
            @remove="removeItem"
            @resolve-location="resolveLocation"
            @calculate-route="calculateRoute"
          />
        </div>

        <HotelCandidateList
          :hotels="hotels"
          :loaded="hotelsLoaded"
          :loading="loadingHotels"
          :parsing="parsingHotelPreference"
          :selected-hotel-name="selectedHotelName"
          :location-options="hotelLocationOptions"
          :parsed-filters="parsedHotelFilters"
          @load="loadHotels"
          @parse-preference="parseHotelPreference"
          @select="selectHotel"
        />
      </template>
    </main>
  </div>
</template>

<style scoped>
.page-shell {
  display: grid;
  min-height: 100vh;
  background: #faf6f1;
  grid-template-columns: 178px minmax(0, 1fr);
}
main {
  display: grid;
  width: min(1180px, calc(100% - 44px));
  margin: 0 auto;
  padding: 34px 0 70px;
  align-content: start;
  gap: 18px;
}
.back {
  width: max-content;
  color: #82746d;
  font-size: 13px;
  text-decoration: none;
}
.message {
  position: sticky;
  z-index: 3;
  top: 10px;
  margin: 0;
  padding: 11px 15px;
  border-radius: 12px;
  background: #edf5ed;
  color: #416749;
  box-shadow: var(--shadow-soft);
  font-size: 13px;
}
.message.error {
  background: #fff0ed;
  color: #a64f52;
}
.loading {
  padding: 60px;
  color: var(--text-muted);
  text-align: center;
}
.item-editor-anchor,
.saved-itinerary {
  scroll-margin-top: 18px;
}
@media (max-width: 900px) {
  .page-shell {
    display: block;
  }
  main {
    padding-top: 22px;
  }
}
</style>
