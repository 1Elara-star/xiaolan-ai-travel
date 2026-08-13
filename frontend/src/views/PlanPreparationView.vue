<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { createPlan } from '@/api/travel'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { HOME_IDEA_KEY } from '@/constants/draft'
import { cities, citySlugs } from '@/data/cities'
import type { CitySlug } from '@/types/city'
import { getResponseMessage } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const citySlug = computed<CitySlug>(() => {
  const value = String(route.query.city ?? '')
  return citySlugs.includes(value as CitySlug) ? (value as CitySlug) : 'xiamen'
})
const city = computed(() => cities[citySlug.value])
const attractionIds = computed(() => String(route.query.attractions ?? '').split(',').filter(Boolean))
const attractions = computed(() => city.value.attractions.filter((item) => attractionIds.value.includes(item.id)))
const today = new Date().toISOString().slice(0, 10)
const form = reactive({
  title: `${city.value.name}旅行`, departureCity: '', destination: city.value.name,
  startDate: '', endDate: '', peopleCount: 1, companionType: '一个人', budget: undefined as number | undefined,
  tripType: '轻松旅行', tripPreferences: localStorage.getItem(HOME_IDEA_KEY) ?? '', specialRequirements: '',
})
const submitting = ref(false)
const message = ref('')

async function submit() {
  message.value = ''
  if (!form.startDate || !form.endDate) { message.value = '请选择出发和结束日期。'; return }
  if (form.endDate < form.startDate) { message.value = '结束日期不能早于出发日期。'; return }
  submitting.value = true
  try {
    await createPlan({ ...form })
    localStorage.removeItem(HOME_IDEA_KEY)
    await router.push({ name: 'plans' })
  } catch (error) {
    message.value = getResponseMessage(error) || '创建行程失败，请稍后重试。'
  } finally { submitting.value = false }
}
</script>

<template>
  <div class="page-shell">
    <HomeSidebar active="plans" />
    <main>
      <RouterLink class="back" :to="{ name: 'city-explore', params: { city: city.slug } }">← 返回{{ city.name }}</RouterLink>
      <section class="card">
        <header><p>新行程</p><h1>先把这趟旅行说清楚</h1><span>这些是基础需求。以后由你接入 AI，再根据画像生成详细方案。</span></header>
        <div class="city"><img :src="city.heroImage" :alt="city.name"><div><small>目的地</small><strong>{{ city.name }}</strong><span>已选 {{ attractions.length }} 个心动地点</span></div></div>
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
          <button class="submit" :disabled="submitting" type="submit">{{ submitting ? '正在保存…' : '保存行程需求' }}</button>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{width:min(920px,calc(100% - 40px));margin:auto;padding:38px 0 70px}.back{color:#8c7e77;font-size:12px;text-decoration:none}.card{margin-top:20px;padding:clamp(25px,5vw,50px);border:1px solid #eadfd9;border-radius:24px;background:#fffaf6;box-shadow:var(--shadow-card)}header p{margin:0;color:var(--coral);font-size:12px}h1{margin:8px 0;font-family:var(--font-display);font-size:clamp(30px,5vw,46px)}header span{color:var(--text-muted);font-size:13px}.city{display:grid;margin:28px 0;overflow:hidden;border-radius:16px;background:#fff;grid-template-columns:180px 1fr}.city img{width:100%;height:125px;object-fit:cover}.city div{display:grid;padding:20px;align-content:center;gap:5px}.city small,.city span{color:var(--text-muted);font-size:12px}.city strong{font-size:24px}form{display:grid;grid-template-columns:1fr 1fr;gap:18px}label{display:grid;gap:7px}label span{font-size:13px;color:#625651}.wide{grid-column:1/-1}input,select,textarea{width:100%;padding:12px 14px;border:1px solid #e7dad3;border-radius:11px;background:#fff;color:var(--text-main);outline:none}textarea{min-height:90px;resize:vertical}.message{grid-column:1/-1;margin:0;color:#b75458;font-size:13px}.submit{grid-column:1/-1;padding:14px;border:0;border-radius:22px;background:var(--coral-strong);color:#fff;cursor:pointer}.submit:disabled{opacity:.6}@media(max-width:900px){.page-shell{display:block}}@media(max-width:600px){form{grid-template-columns:1fr}.wide{grid-column:auto}.city{grid-template-columns:1fr}}
</style>
