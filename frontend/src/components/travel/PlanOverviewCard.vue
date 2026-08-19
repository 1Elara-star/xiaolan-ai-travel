<script setup lang="ts">
import { reactive, ref, watch } from 'vue'

import type { TravelPlan, TravelPlanRequest } from '@/types/travel'

const props = defineProps<{
  plan: TravelPlan
  saving?: boolean
  generating?: boolean
}>()

const emit = defineEmits<{
  save: [request: TravelPlanRequest]
  generate: []
}>()

const editing = ref(false)
const form = reactive<TravelPlanRequest>({ ...props.plan })

watch(
  () => props.plan,
  (plan) => Object.assign(form, plan),
)

function submit() {
  emit('save', {
    title: form.title.trim(),
    departureCity: form.departureCity.trim(),
    destination: form.destination.trim(),
    startDate: form.startDate,
    endDate: form.endDate,
    peopleCount: form.peopleCount,
    companionType: form.companionType?.trim() || undefined,
    budget: form.budget,
    tripType: form.tripType?.trim() || undefined,
    tripPreferences: form.tripPreferences?.trim() || undefined,
    specialRequirements: form.specialRequirements?.trim() || undefined,
  })
  editing.value = false
}
</script>

<template>
  <section class="overview-card">
    <div class="overview-main">
      <div class="eyebrow">
        <span>{{ plan.tripStatus === 'PLANNING' ? '准备中的旅行' : plan.tripStatus }}</span>
        <button type="button" @click="editing = !editing">{{ editing ? '收起编辑' : '编辑需求' }}</button>
      </div>
      <h1>{{ plan.title }}</h1>
      <p class="route">{{ plan.departureCity }} → {{ plan.destination }}</p>
      <div class="meta">
        <span>{{ plan.startDate }} — {{ plan.endDate }}</span>
        <span>{{ plan.travelDays }} 天</span>
        <span>{{ plan.peopleCount }} 人</span>
        <span v-if="plan.budget">预算 ¥{{ plan.budget }}</span>
      </div>
      <p class="preference">{{ plan.tripPreferences || '还没有补充这次旅行的偏好。' }}</p>
    </div>

    <aside>
      <strong>DeepSeek 行程生成</strong>
      <p>后端已经能够生成结构化行程并保存节点，但目前还不会自动串联高德校验。</p>
      <button type="button" :disabled="generating" @click="emit('generate')">
        {{ generating ? '正在生成…' : '调用 DeepSeek 生成' }}
      </button>
    </aside>

    <form v-if="editing" @submit.prevent="submit">
      <label class="wide"><span>行程名称</span><input v-model="form.title" maxlength="100" required /></label>
      <label><span>出发城市</span><input v-model="form.departureCity" maxlength="100" required /></label>
      <label><span>目的地</span><input v-model="form.destination" maxlength="100" required /></label>
      <label><span>开始日期</span><input v-model="form.startDate" type="date" required /></label>
      <label><span>结束日期</span><input v-model="form.endDate" type="date" required /></label>
      <label><span>人数</span><input v-model.number="form.peopleCount" type="number" min="1" required /></label>
      <label><span>同行方式</span><input v-model="form.companionType" maxlength="50" /></label>
      <label><span>预算</span><input v-model.number="form.budget" type="number" min="1" /></label>
      <label><span>旅行类型</span><input v-model="form.tripType" maxlength="50" /></label>
      <label class="wide"><span>旅行偏好</span><textarea v-model="form.tripPreferences" maxlength="500" /></label>
      <label class="wide"><span>特殊要求</span><textarea v-model="form.specialRequirements" maxlength="1000" /></label>
      <button class="save" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存修改' }}</button>
    </form>
  </section>
</template>

<style scoped>
.overview-card{display:grid;padding:28px;border:1px solid #eadfd9;border-radius:22px;background:#fffaf6;box-shadow:var(--shadow-card);grid-template-columns:minmax(0,1fr) 280px;gap:26px}.eyebrow{display:flex;align-items:center;gap:12px}.eyebrow span{color:var(--coral);font-size:12px}.eyebrow button{padding:0;border:0;background:transparent;color:#8d7e77;cursor:pointer;font-size:12px}h1{margin:10px 0 5px;font-family:var(--font-display);font-size:clamp(31px,5vw,46px)}.route{margin:0;font-size:17px}.meta{display:flex;margin-top:18px;color:var(--text-muted);flex-wrap:wrap;gap:8px 17px;font-size:13px}.preference{margin:18px 0 0;color:#6d615c;font-size:13px;line-height:1.7}aside{padding:19px;border-radius:16px;background:#f9ebe6;align-self:start}aside strong{font-size:15px}aside p{margin:9px 0 16px;color:#776862;font-size:12px;line-height:1.65}aside button{width:100%;padding:11px;border:0;border-radius:20px;background:var(--coral-strong);color:#fff;cursor:pointer}aside button:disabled{cursor:not-allowed;opacity:.6}form{display:grid;padding-top:23px;border-top:1px solid #eadfd9;grid-column:1/-1;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}label{display:grid;gap:6px}label span{color:#625651;font-size:12px}.wide,.save{grid-column:1/-1}input,textarea{padding:11px 12px;border:1px solid #e7dad3;border-radius:10px;background:#fff;color:var(--text-main)}textarea{min-height:72px;resize:vertical}.save{padding:12px;border:0;border-radius:20px;background:#554943;color:#fff;cursor:pointer}.save:disabled{opacity:.6}
@media(max-width:760px){.overview-card{grid-template-columns:1fr}form{grid-column:auto}}@media(max-width:520px){form{grid-template-columns:1fr}.wide,.save{grid-column:auto}}
</style>
