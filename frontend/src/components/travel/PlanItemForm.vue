<script setup lang="ts">
import { computed, reactive, watch } from 'vue'

import type { TravelPlanItem, TravelPlanItemRequest, TravelItemType } from '@/types/travel'

const props = defineProps<{
  planDays: number
  item?: TravelPlanItem | null
  saving?: boolean
}>()

const emit = defineEmits<{
  submit: [request: TravelPlanItemRequest]
  cancel: []
}>()

const itemTypes: Array<{ value: TravelItemType; label: string }> = [
  { value: 'ATTRACTION', label: '景点' },
  { value: 'FOOD', label: '餐饮' },
  { value: 'SHOPPING', label: '购物' },
  { value: 'HOTEL', label: '酒店' },
  { value: 'EVENT', label: '活动' },
  { value: 'REST', label: '休息' },
  { value: 'OTHER', label: '其他' },
]

const form = reactive<TravelPlanItemRequest>(emptyForm())
const title = computed(() => (props.item ? '编辑行程节点' : '添加行程节点'))

watch(
  () => props.item,
  (item) => Object.assign(form, item ? requestFromItem(item) : emptyForm()),
  { immediate: true },
)

function emptyForm(): TravelPlanItemRequest {
  return {
    dayNumber: 1,
    itemOrder: 1,
    itemType: 'ATTRACTION',
    attractionId: null,
    placeName: '',
    address: null,
    longitude: null,
    latitude: null,
    startTime: null,
    endTime: null,
    endDayOffset: 0,
    transportMode: null,
    distanceFromPrev: null,
    travelTimeFromPrev: null,
    description: null,
  }
}

function requestFromItem(item: TravelPlanItem): TravelPlanItemRequest {
  return {
    dayNumber: item.dayNumber,
    itemOrder: item.itemOrder,
    itemType: item.itemType,
    attractionId: item.attractionId ?? null,
    placeName: item.placeName,
    address: item.address ?? null,
    longitude: item.longitude ?? null,
    latitude: item.latitude ?? null,
    startTime: item.startTime ?? null,
    endTime: item.endTime ?? null,
    endDayOffset: item.endDayOffset ?? 0,
    transportMode: item.transportMode ?? null,
    distanceFromPrev: item.distanceFromPrev ?? null,
    travelTimeFromPrev: item.travelTimeFromPrev ?? null,
    description: item.description ?? null,
  }
}

function submit() {
  emit('submit', {
    ...form,
    placeName: form.placeName.trim(),
    description: form.description?.trim() || null,
    startTime: form.startTime || null,
    endTime: form.endTime || null,
  })
}
</script>

<template>
  <section class="editor-card">
    <header>
      <div>
        <span>{{ item ? '修改已有安排' : '手动补充安排' }}</span>
        <h2>{{ title }}</h2>
      </div>
      <button type="button" class="close" aria-label="关闭编辑器" @click="emit('cancel')">×</button>
    </header>

    <form @submit.prevent="submit">
      <label>
        <span>第几天</span>
        <input v-model.number="form.dayNumber" type="number" min="1" :max="planDays" required />
      </label>
      <label>
        <span>当天顺序</span>
        <input v-model.number="form.itemOrder" type="number" min="1" required />
      </label>
      <label>
        <span>节点类型</span>
        <select v-model="form.itemType">
          <option v-for="type in itemTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
      </label>
      <label class="place-field">
        <span>地点名称</span>
        <input v-model.trim="form.placeName" maxlength="150" required placeholder="例如：鼓浪屿" />
      </label>
      <label>
        <span>开始时间</span>
        <input v-model="form.startTime" type="time" />
      </label>
      <label>
        <span>结束时间</span>
        <input v-model="form.endTime" type="time" />
      </label>
      <label class="cross-day">
        <input
          :checked="form.endDayOffset === 1"
          type="checkbox"
          @change="form.endDayOffset = ($event.target as HTMLInputElement).checked ? 1 : 0"
        />
        <span>结束时间在第二天（例如夜班交通或住宿）</span>
      </label>
      <label class="description-field">
        <span>节点说明</span>
        <textarea
          v-model="form.description"
          maxlength="500"
          placeholder="这段安排有什么值得注意的地方"
        />
      </label>
      <div class="form-actions">
        <button type="button" class="secondary" @click="emit('cancel')">取消</button>
        <button type="submit" class="primary" :disabled="saving">
          {{ saving ? '保存中…' : '保存节点' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.editor-card{padding:24px;border:1px solid #eadfd9;border-radius:18px;background:#fffaf6;box-shadow:var(--shadow-soft)}
header{display:flex;margin-bottom:20px;align-items:flex-start;justify-content:space-between}header span{color:var(--coral);font-size:12px}h2{margin:5px 0 0;font-size:21px}.close{border:0;background:transparent;color:#9e8f88;cursor:pointer;font-size:25px}
form{display:grid;grid-template-columns:120px 120px 160px minmax(220px,1fr);gap:15px}label{display:grid;gap:7px}label>span{color:#625651;font-size:13px}.place-field{min-width:0}.cross-day,.description-field,.form-actions{grid-column:1/-1}.cross-day{display:flex;align-items:center;gap:9px}.cross-day input{width:auto}.cross-day span{font-size:13px}
input,select,textarea{width:100%;padding:11px 12px;border:1px solid #e7dad3;border-radius:10px;background:#fff;color:var(--text-main);outline:none}textarea{min-height:78px;resize:vertical}.form-actions{display:flex;justify-content:flex-end;gap:9px}.form-actions button{padding:10px 18px;border-radius:20px;cursor:pointer}.secondary{border:1px solid #e5d7d0;background:#fff}.primary{border:0;background:var(--coral-strong);color:white}.primary:disabled{cursor:not-allowed;opacity:.6}
@media(max-width:760px){form{grid-template-columns:1fr 1fr}.place-field{grid-column:1/-1}}@media(max-width:520px){form{grid-template-columns:1fr}.place-field,.cross-day,.description-field,.form-actions{grid-column:auto}}
</style>
