<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import * as travelApi from '@/api/travel'
import HotelCandidateList from '@/components/travel/HotelCandidateList.vue'
import PlanItemForm from '@/components/travel/PlanItemForm.vue'
import PlanOverviewCard from '@/components/travel/PlanOverviewCard.vue'
import PlanTimeline from '@/components/travel/PlanTimeline.vue'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import type {
  HotelCandidate,
  TravelMode,
  TravelPlan,
  TravelPlanItem,
  TravelPlanItemRequest,
  TravelPlanRequest,
} from '@/types/travel'
import { getResponseMessage } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.id))
const plan = ref<TravelPlan | null>(null)
const items = ref<TravelPlanItem[]>([])
const hotels = ref<HotelCandidate[]>([])
const hotelsLoaded = ref(false)
const loading = ref(true)
const savingPlan = ref(false)
const savingItem = ref(false)
const generating = ref(false)
const loadingHotels = ref(false)
const busyAction = ref('')
const editingItem = ref<TravelPlanItem | null>(null)
const showItemForm = ref(false)
const itemEditor = ref<HTMLElement | null>(null)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

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

async function generateWithAi() {
  const warning = items.value.length
    ? '当前计划已经有行程节点，后端会拒绝自动覆盖。仍要尝试调用吗？'
    : '这会真实调用 DeepSeek 并消耗接口额度，确定继续吗？'
  if (!window.confirm(warning)) return

  generating.value = true
  setMessage('')
  try {
    const result = await travelApi.generatePlanWithAi(planId.value)
    items.value = await travelApi.listPlanItems(planId.value)
    setMessage(result.summary || `已生成 ${result.travelDays} 天结构化行程。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || 'DeepSeek 行程生成失败。', 'error')
  } finally {
    generating.value = false
  }
}

async function resolveLocation(item: TravelPlanItem) {
  if (!window.confirm(`将调用一次高德，为“${item.placeName}”匹配真实地点，是否继续？`)) return
  busyAction.value = `location-${item.id}`
  try {
    const result = await travelApi.resolveItemLocation(planId.value, item.id)
    items.value = await travelApi.listPlanItems(planId.value)
    setMessage(`已匹配：${result.poiName}（${result.source}）`)
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

async function loadHotels() {
  if (!window.confirm('将真实调用飞猪酒店查询，是否继续？')) return
  loadingHotels.value = true
  try {
    hotels.value = await travelApi.listHotelCandidates(planId.value)
    hotelsLoaded.value = true
    setMessage(`已获得 ${hotels.value.length} 个酒店候选。`)
  } catch (error) {
    setMessage(getResponseMessage(error) || '酒店候选查询失败。', 'error')
  } finally {
    loadingHotels.value = false
  }
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
          @save="savePlan"
          @generate="generateWithAi"
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

        <PlanTimeline
          :items="items"
          :travel-days="plan.travelDays"
          :busy-action="busyAction"
          @add="openNewItem"
          @edit="openEditItem"
          @remove="removeItem"
          @resolve-location="resolveLocation"
          @calculate-route="calculateRoute"
        />

        <HotelCandidateList
          :hotels="hotels"
          :loaded="hotelsLoaded"
          :loading="loadingHotels"
          @load="loadHotels"
        />
      </template>
    </main>
  </div>
</template>

<style scoped>
.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{display:grid;width:min(1180px,calc(100% - 44px));margin:0 auto;padding:34px 0 70px;align-content:start;gap:18px}.back{width:max-content;color:#82746d;font-size:13px;text-decoration:none}.message{position:sticky;z-index:3;top:10px;margin:0;padding:11px 15px;border-radius:12px;background:#edf5ed;color:#416749;box-shadow:var(--shadow-soft);font-size:13px}.message.error{background:#fff0ed;color:#a64f52}.loading{padding:60px;color:var(--text-muted);text-align:center}.item-editor-anchor{scroll-margin-top:18px}
@media(max-width:900px){.page-shell{display:block}main{padding-top:22px}}
</style>
