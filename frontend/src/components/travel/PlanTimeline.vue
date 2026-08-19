<script setup lang="ts">
import { computed, reactive } from 'vue'

import type { TravelMode, TravelPlanItem } from '@/types/travel'

const props = defineProps<{
  items: TravelPlanItem[]
  travelDays: number
  busyAction?: string
}>()

const emit = defineEmits<{
  add: []
  edit: [item: TravelPlanItem]
  remove: [item: TravelPlanItem]
  resolveLocation: [item: TravelPlanItem]
  calculateRoute: [item: TravelPlanItem, mode: TravelMode]
}>()

const routeModes = reactive<Record<number, TravelMode>>({})
const firstItemId = computed(() => props.items[0]?.id ?? null)
const days = computed(() =>
  Array.from({ length: props.travelDays }, (_, index) => ({
    dayNumber: index + 1,
    items: props.items.filter((item) => item.dayNumber === index + 1),
  })),
)

const typeLabels: Record<string, string> = {
  ATTRACTION: '景点',
  FOOD: '餐饮',
  SHOPPING: '购物',
  HOTEL: '酒店',
  EVENT: '活动',
  REST: '休息',
  OTHER: '其他',
}

const modeLabels: Record<TravelMode, string> = {
  WALKING: '步行',
  DRIVING: '驾车',
  BICYCLING: '骑行',
  TRANSIT: '公交',
}

function selectedMode(item: TravelPlanItem): TravelMode {
  const savedMode = item.transportMode as TravelMode | null | undefined
  return routeModes[item.id] ?? savedMode ?? 'WALKING'
}

function canResolve(item: TravelPlanItem) {
  return item.itemType !== 'REST' && item.placeName !== '待推荐酒店'
}

function timeText(item: TravelPlanItem) {
  if (!item.startTime && !item.endTime) return '时间待定'
  const endSuffix = item.endDayOffset === 1 ? '（次日）' : ''
  return `${item.startTime ?? '待定'} — ${item.endTime ?? '待定'}${endSuffix}`
}
</script>

<template>
  <section class="timeline-card">
    <header>
      <div>
        <span>结构化行程节点</span>
        <h2>详细行程</h2>
      </div>
      <button type="button" class="add-button" @click="emit('add')">＋ 添加节点</button>
    </header>

    <div v-if="items.length === 0" class="empty">
      <strong>还没有详细行程</strong>
      <p>可以手动添加，也可以在上方明确点击 DeepSeek 生成。</p>
    </div>

    <div v-else class="day-list">
      <section v-for="day in days" :key="day.dayNumber" class="day-block">
        <div class="day-heading">
          <strong>Day {{ day.dayNumber }}</strong>
          <span>{{ day.items.length }} 个节点</span>
        </div>

        <div v-if="day.items.length === 0" class="day-empty">这一天暂时没有安排</div>

        <article v-for="item in day.items" :key="item.id" class="item-card">
          <div class="time-column">
            <strong>{{ timeText(item) }}</strong>
            <span>顺序 {{ item.itemOrder }}</span>
          </div>

          <div class="item-main">
            <div class="item-title">
              <span>{{ typeLabels[item.itemType] ?? item.itemType }}</span>
              <h3>{{ item.placeName }}</h3>
            </div>
            <p v-if="item.description">{{ item.description }}</p>
            <div class="facts">
              <span v-if="item.address">地址：{{ item.address }}</span>
              <span v-if="item.longitude != null && item.latitude != null">
                已匹配真实坐标
              </span>
              <span v-if="item.travelTimeFromPrev != null">
                从上一站{{ modeLabels[item.transportMode as TravelMode] ?? item.transportMode }}
                {{ item.travelTimeFromPrev }} 分钟 · {{ item.distanceFromPrev ?? 0 }} 米
              </span>
            </div>
          </div>

          <div class="item-actions">
            <button
              type="button"
              :disabled="!canResolve(item) || busyAction === `location-${item.id}`"
              @click="emit('resolveLocation', item)"
            >
              {{ busyAction === `location-${item.id}` ? '匹配中…' : '匹配地点' }}
            </button>
            <div v-if="item.id !== firstItemId" class="route-action">
              <select
                :value="selectedMode(item)"
                aria-label="交通方式"
                @change="routeModes[item.id] = ($event.target as HTMLSelectElement).value as TravelMode"
              >
                <option v-for="(label, value) in modeLabels" :key="value" :value="value">
                  {{ label }}
                </option>
              </select>
              <button
                type="button"
                :disabled="busyAction === `route-${item.id}`"
                @click="emit('calculateRoute', item, selectedMode(item))"
              >
                {{ busyAction === `route-${item.id}` ? '计算中…' : '计算路线' }}
              </button>
            </div>
            <button type="button" @click="emit('edit', item)">编辑</button>
            <button type="button" class="danger" @click="emit('remove', item)">删除</button>
          </div>
        </article>
      </section>
    </div>
  </section>
</template>

<style scoped>
.timeline-card{padding:24px;border:1px solid #eadfd9;border-radius:20px;background:#fffaf6;box-shadow:var(--shadow-soft)}header{display:flex;align-items:flex-end;justify-content:space-between}header span{color:var(--coral);font-size:12px}h2{margin:5px 0 0;font-size:22px}.add-button{padding:10px 15px;border:0;border-radius:20px;background:var(--coral-strong);color:#fff;cursor:pointer}.empty{padding:48px 10px;color:var(--text-muted);text-align:center}.empty p{margin:8px 0 0;font-size:13px}.day-list{display:grid;margin-top:24px;gap:22px}.day-block{display:grid;gap:10px}.day-heading{display:flex;padding-bottom:8px;border-bottom:1px solid #eee1da;align-items:center;justify-content:space-between}.day-heading strong{font-size:18px}.day-heading span,.day-empty{color:var(--text-muted);font-size:12px}.day-empty{padding:16px 0}.item-card{display:grid;padding:17px;border:1px solid #eee2dc;border-radius:15px;background:#fff;grid-template-columns:145px minmax(0,1fr) auto;gap:18px}.time-column{display:grid;align-content:start;gap:5px}.time-column strong{font-size:13px}.time-column span{color:var(--text-muted);font-size:12px}.item-title{display:flex;align-items:center;gap:9px}.item-title span{padding:3px 7px;border-radius:10px;background:#f9e5df;color:#a95e5e;font-size:11px}.item-title h3{margin:0;font-size:17px}.item-main>p{margin:8px 0;color:#6d615c;font-size:13px;line-height:1.7}.facts{display:flex;margin-top:9px;color:var(--text-muted);flex-wrap:wrap;gap:7px 15px;font-size:12px}.item-actions{display:flex;max-width:265px;align-content:start;justify-content:flex-end;flex-wrap:wrap;gap:7px}.item-actions button,.item-actions select{min-height:34px;padding:7px 10px;border:1px solid #e6d9d2;border-radius:10px;background:#fff;cursor:pointer;font-size:12px}.item-actions button:disabled{cursor:not-allowed;opacity:.5}.route-action{display:flex;gap:5px}.danger{color:#ae575b}.day-empty{border:1px dashed #eadfd9;border-radius:12px;text-align:center}
@media(max-width:900px){.item-card{grid-template-columns:1fr}.item-actions{max-width:none;justify-content:flex-start}}@media(max-width:560px){.timeline-card{padding:18px}.item-card{padding:14px}.route-action{width:100%}.route-action select,.route-action button{flex:1}}
</style>
