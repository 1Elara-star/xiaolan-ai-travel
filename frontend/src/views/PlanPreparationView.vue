<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import * as exploreApi from '@/api/explore'
import { createPlan, createPlanItem } from '@/api/travel'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { HOME_IDEA_KEY, PLAN_DRAFT_KEY, readJsonStorage } from '@/constants/draft'
import { cities, citySlugs } from '@/data/cities'
import { useAuthStore } from '@/stores/auth'
import type { Attraction, CityExploreData, CitySlug } from '@/types/city'
import type { TravelPlanRequest } from '@/types/travel'
import { getResponseMessage } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const citySlug = computed<CitySlug>(() => {
  const value = String(route.query.city ?? '')
  return citySlugs.includes(value as CitySlug) ? (value as CitySlug) : 'xiamen'
})
const city = ref<CityExploreData>(cities[citySlug.value])
const attractionIds = computed(() => String(route.query.attractions ?? '').split(',').filter(Boolean))
const attractions = computed(() =>
  city.value.attractions.filter((item) => attractionIds.value.includes(String(item.id))),
)
const today = new Date().toISOString().slice(0, 10)
const savedDraft = readJsonStorage<PlanDraft | null>(PLAN_DRAFT_KEY, null)
const form = reactive<TravelPlanRequest>(
  savedDraft?.citySlug === citySlug.value
    ? savedDraft.form
    : createEmptyForm(cities[citySlug.value].name),
)
const submitting = ref(false)
const loadingCity = ref(false)
const message = ref('')

interface PlanDraft {
  citySlug: CitySlug
  form: TravelPlanRequest
}

watch(citySlug, loadCity, { immediate: true })

function createEmptyForm(cityName: string): TravelPlanRequest {
  return {
    title: `${cityName}旅行`,
    departureCity: '',
    destination: cityName,
    startDate: '',
    endDate: '',
    peopleCount: 1,
    companionType: '一个人',
    budget: undefined,
    tripType: '轻松旅行',
    tripPreferences: localStorage.getItem(HOME_IDEA_KEY) ?? '',
    specialRequirements: '',
  }
}

async function loadCity() {
  const fallback = cities[citySlug.value]
  city.value = fallback
  loadingCity.value = true

  try {
    city.value = await exploreApi.getCity(citySlug.value)
  } catch {
    city.value = fallback
  } finally {
    loadingCity.value = false
  }

  if (!savedDraft || savedDraft.citySlug !== citySlug.value) {
    form.destination = city.value.name
    form.title = `${city.value.name}旅行`
  }
}

async function submit() {
  message.value = ''
  if (!form.startDate || !form.endDate) {
    message.value = '请选择出发和结束日期。'
    return
  }
  if (form.endDate < form.startDate) {
    message.value = '结束日期不能早于出发日期。'
    return
  }

  const request = normalizedRequest()
  if (!authStore.isAuthenticated) {
    localStorage.setItem(
      PLAN_DRAFT_KEY,
      JSON.stringify({ citySlug: citySlug.value, form: request } satisfies PlanDraft),
    )
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  submitting.value = true
  try {
    const plan = await createPlan(request)
    const itemResults = await Promise.allSettled(
      attractions.value.map((attraction, index) =>
        createPlanItem(plan.id, {
          dayNumber: 1,
          itemOrder: index + 1,
          itemType: 'ATTRACTION',
          attractionId: numericId(attraction),
          placeName: attraction.name,
          address: attraction.address ?? null,
          longitude: attraction.longitude ?? null,
          latitude: attraction.latitude ?? null,
          description: '从城市收藏带入，具体日期和时间待安排。',
        }),
      ),
    )

    localStorage.removeItem(HOME_IDEA_KEY)
    localStorage.removeItem(PLAN_DRAFT_KEY)

    const failedItems = itemResults.filter((result) => result.status === 'rejected').length
    if (failedItems > 0) {
      sessionStorage.setItem(
        'xiaolan-plan-notice',
        `${failedItems} 个收藏地点未能带入，可以在详细行程中手动补充。`,
      )
    }
    await router.push({ name: 'plan-detail', params: { id: plan.id } })
  } catch (error) {
    message.value = getResponseMessage(error) || '创建行程失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

function normalizedRequest(): TravelPlanRequest {
  return {
    ...form,
    title: form.title.trim(),
    departureCity: form.departureCity.trim(),
    destination: form.destination.trim(),
    companionType: form.companionType?.trim() || undefined,
    tripType: form.tripType?.trim() || undefined,
    tripPreferences: form.tripPreferences?.trim() || undefined,
    specialRequirements: form.specialRequirements?.trim() || undefined,
  }
}

function numericId(attraction: Attraction) {
  const id = Number(attraction.id)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
</script>

<template>
  <div class="page-shell">
    <HomeSidebar active="plans" />
    <main>
      <RouterLink class="back" :to="{ name: 'city-explore', params: { city: city.slug } }">← 返回{{ city.name }}</RouterLink>
      <section class="card">
        <header><p>新行程</p><h1>先把这趟旅行说清楚</h1><span>这些是基础需求。以后由你接入 AI，再根据画像生成详细方案。</span></header>
        <div class="city"><img :src="city.heroImage" :alt="city.name"><div><small>目的地</small><strong>{{ city.name }}</strong><span>{{ loadingCity ? '正在读取景点…' : `已选 ${attractions.length} 个心动地点` }}</span></div></div>
        <div v-if="attractions.length" class="selected-attractions">
          <span v-for="attraction in attractions" :key="attraction.id">{{ attraction.name }}</span>
        </div>
        <form @submit.prevent="submit">
          <label class="wide"><span>行程名称</span><input v-model.trim="form.title" required maxlength="100"></label>
          <label><span>从哪里出发</span><input v-model.trim="form.departureCity" required maxlength="100" placeholder="例如：广州"></label>
          <label><span>目的地</span><input v-model.trim="form.destination" required maxlength="100"></label>
          <label><span>出发日期</span><input v-model="form.startDate" :min="today" type="date" required></label>
          <label><span>结束日期</span><input v-model="form.endDate" :min="form.startDate || today" type="date" required></label>
          <label><span>人数</span><input v-model.number="form.peopleCount" min="1" type="number" required></label>
          <label><span>同行方式</span><select v-model="form.companionType"><option>一个人</option><option>朋友</option><option>伴侣</option><option>家人</option></select></label>
          <label><span>总预算（元）</span><input v-model.number="form.budget" min="1" type="number" placeholder="可选"></label>
          <label><span>旅行类型</span><select v-model="form.tripType"><option>轻松旅行</option><option>深度人文</option><option>美食旅行</option><option>摄影旅行</option><option>亲子旅行</option></select></label>
          <label class="wide"><span>这次特别想要什么</span><textarea v-model.trim="form.tripPreferences" maxlength="500" placeholder="例如：想看海，不想赶行程"></textarea></label>
          <label class="wide"><span>需要特别注意</span><textarea v-model.trim="form.specialRequirements" maxlength="1000" placeholder="饮食禁忌、行动不便或其他说明"></textarea></label>
          <p v-if="message" class="message" role="alert">{{ message }}</p>
          <button class="submit" :disabled="submitting" type="submit">{{ submitting ? '正在保存…' : authStore.isAuthenticated ? '保存行程需求' : '登录后保存行程' }}</button>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page-shell{display:grid;min-width:0;min-height:100vh;overflow-x:hidden;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{width:min(920px,calc(100% - 40px));max-width:100%;margin:auto;padding:38px 0 70px}.back{color:#8c7e77;font-size:12px;text-decoration:none}.card{min-width:0;margin-top:20px;padding:clamp(25px,5vw,50px);overflow:hidden;border:1px solid #eadfd9;border-radius:24px;background:#fffaf6;box-shadow:var(--shadow-card)}header p{margin:0;color:var(--coral);font-size:12px}h1{margin:8px 0;font-family:var(--font-display);font-size:clamp(30px,5vw,46px);overflow-wrap:anywhere}header span{color:var(--text-muted);font-size:13px}.city{display:grid;margin:28px 0 12px;overflow:hidden;border-radius:16px;background:#fff;grid-template-columns:180px 1fr}.city img{width:100%;height:125px;object-fit:cover}.city div{display:grid;min-width:0;padding:20px;align-content:center;gap:5px}.city small,.city span{color:var(--text-muted);font-size:12px}.city strong{font-size:24px}.selected-attractions{display:flex;margin:0 0 24px;flex-wrap:wrap;gap:7px}.selected-attractions span{padding:6px 10px;border-radius:14px;background:#f9e5df;color:#8f5556;font-size:12px}form{display:grid;min-width:0;grid-template-columns:1fr 1fr;gap:18px}label{display:grid;min-width:0;gap:7px}label span{font-size:13px;color:#625651}.wide{grid-column:1/-1}input,select,textarea{width:100%;min-width:0;padding:12px 14px;border:1px solid #e7dad3;border-radius:11px;background:#fff;color:var(--text-main);outline:none}textarea{min-height:90px;resize:vertical}.message{grid-column:1/-1;margin:0;color:#b75458;font-size:13px}.submit{grid-column:1/-1;padding:14px;border:0;border-radius:22px;background:var(--coral-strong);color:#fff;cursor:pointer}.submit:disabled{opacity:.6}@media(max-width:900px){.page-shell{display:block}}@media(max-width:600px){main{width:calc(100% - 24px)}.card{padding:24px}.city{grid-template-columns:1fr}.city img{height:150px}form{grid-template-columns:1fr}.wide{grid-column:auto}}
</style>
