<script setup lang="ts">
import { computed, reactive } from 'vue'

import type { TravelItemLocationResult, TravelMode, TravelPlanItem } from '@/types/travel'

const props = defineProps<{
  items: TravelPlanItem[]
  travelDays: number
  busyAction?: string
  locationResults?: Record<number, TravelItemLocationResult>
}>()

const emit = defineEmits<{
  add: []
  edit: [item: TravelPlanItem]
  remove: [item: TravelPlanItem]
  resolveLocation: [item: TravelPlanItem]
  calculateRoute: [item: TravelPlanItem, mode: TravelMode]
}>()

const routeModes = reactive<Record<number, TravelMode | ''>>({})
const failedImages = reactive<Record<number, boolean>>({})

const days = computed(() =>
  Array.from({ length: props.travelDays }, (_, index) => ({
    dayNumber: index + 1,
    items: props.items
      .filter((item) => item.dayNumber === index + 1)
      .sort((left, right) => left.itemOrder - right.itemOrder || left.id - right.id),
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
  BICYCLING: '骑行',
  DRIVING: '驾车',
  TRANSIT: '公交',
}

function selectedMode(item: TravelPlanItem): TravelMode | '' {
  const savedMode = item.transportMode as TravelMode | null | undefined
  return routeModes[item.id] ?? savedMode ?? ''
}

function canResolve(item: TravelPlanItem) {
  return item.itemType !== 'REST' && item.placeName !== '待推荐酒店'
}

function hasCoordinates(item: TravelPlanItem) {
  return item.longitude != null && item.latitude != null
}

function matchedName(item: TravelPlanItem) {
  return props.locationResults?.[item.id]?.poiName || item.placeName
}

function matchedAddress(item: TravelPlanItem) {
  return props.locationResults?.[item.id]?.address || item.address || '暂未返回详细门牌地址'
}

function matchedLongitude(item: TravelPlanItem) {
  return props.locationResults?.[item.id]?.longitude ?? item.longitude
}

function matchedLatitude(item: TravelPlanItem) {
  return props.locationResults?.[item.id]?.latitude ?? item.latitude
}

function itemImage(item: TravelPlanItem) {
  if (failedImages[item.id]) return null
  return props.locationResults?.[item.id]?.imageUrl || item.imageUrl || null
}

function amapUrl(item: TravelPlanItem) {
  const longitude = matchedLongitude(item)
  const latitude = matchedLatitude(item)
  if (longitude == null || latitude == null) return '#'
  const position = `${longitude},${latitude}`
  return `https://uri.amap.com/marker?position=${encodeURIComponent(position)}&name=${encodeURIComponent(matchedName(item))}&src=xiaolan&coordinate=gaode&callnative=0`
}

function calculateRoute(item: TravelPlanItem) {
  const mode = selectedMode(item)
  if (mode) emit('calculateRoute', item, mode)
}

function timeText(item: TravelPlanItem) {
  if (!item.startTime && !item.endTime) return '时间待定'
  const endSuffix = item.endDayOffset === 1 ? '（次日）' : ''
  return `${item.startTime ?? '待定'} — ${item.endTime ?? '待定'}${endSuffix}`
}

function formatDistance(distance: number) {
  return distance >= 1000 ? `${(distance / 1000).toFixed(1)} 公里` : `${distance} 米`
}
</script>

<template>
  <section class="timeline-card">
    <header>
      <div>
        <span>已经采用的节点</span>
        <h2>详细行程</h2>
      </div>
      <button type="button" class="add-button" @click="emit('add')">＋ 添加节点</button>
    </header>

    <div v-if="items.length === 0" class="empty">
      <strong>还没有详细行程</strong>
      <p>可以手动添加，也可以从上方候选方案中选择满意的节点。</p>
    </div>

    <div v-else class="day-list">
      <section v-for="day in days" :key="day.dayNumber" class="day-block">
        <div class="day-heading">
          <strong>Day {{ day.dayNumber }}</strong>
          <span>{{ day.items.length }} 个节点</span>
        </div>

        <div v-if="day.items.length === 0" class="day-empty">这一天暂时没有安排</div>

        <template v-for="(item, itemIndex) in day.items" :key="item.id">
          <div v-if="itemIndex > 0" class="route-connector">
            <div class="route-mark" aria-hidden="true"><span>↓</span></div>
            <div class="route-content">
              <div class="route-copy">
                <strong> {{ day.items[itemIndex - 1]?.placeName }} → {{ item.placeName }} </strong>
                <span v-if="item.straightLineDistanceFromPrev != null" class="distance-preview">
                  直线距离约
                  {{ formatDistance(item.straightLineDistanceFromPrev) }}，请根据距离选择出行方式
                </span>
                <span
                  v-else-if="hasCoordinates(day.items[itemIndex - 1]!) && hasCoordinates(item)"
                  class="distance-preview"
                >
                  两个地点都已定位，距离预览暂不可用
                </span>
                <span v-else>先定位两个地点，才能预览距离和计算路线。</span>
                <span
                  v-if="item.distanceFromPrev != null && item.travelTimeFromPrev != null"
                  class="route-result"
                >
                  当前路线：{{
                    modeLabels[item.transportMode as TravelMode] ?? item.transportMode
                  }}
                  · {{ formatDistance(item.distanceFromPrev) }} · 约
                  {{ item.travelTimeFromPrev }} 分钟
                </span>
              </div>

              <div
                v-if="hasCoordinates(day.items[itemIndex - 1]!) && hasCoordinates(item)"
                class="route-controls"
              >
                <label :for="`route-mode-${item.id}`">出行方式</label>
                <select
                  :id="`route-mode-${item.id}`"
                  :value="selectedMode(item)"
                  aria-label="交通方式"
                  @change="
                    routeModes[item.id] = ($event.target as HTMLSelectElement).value as TravelMode
                  "
                >
                  <option value="" disabled>选择交通方式</option>
                  <option v-for="(label, value) in modeLabels" :key="value" :value="value">
                    {{ label }}
                  </option>
                </select>
                <button
                  type="button"
                  :disabled="!selectedMode(item) || busyAction === `route-${item.id}`"
                  @click="calculateRoute(item)"
                >
                  {{
                    busyAction === `route-${item.id}`
                      ? '计算中…'
                      : item.distanceFromPrev != null
                        ? '按此方式重新计算'
                        : '查询实际路线'
                  }}
                </button>
              </div>

              <div v-else class="locate-controls">
                <button
                  v-if="
                    !hasCoordinates(day.items[itemIndex - 1]!) &&
                    canResolve(day.items[itemIndex - 1]!)
                  "
                  type="button"
                  :disabled="busyAction === `location-${day.items[itemIndex - 1]!.id}`"
                  @click="emit('resolveLocation', day.items[itemIndex - 1]!)"
                >
                  定位“{{ day.items[itemIndex - 1]?.placeName }}”
                </button>
                <button
                  v-if="!hasCoordinates(item) && canResolve(item)"
                  type="button"
                  :disabled="busyAction === `location-${item.id}`"
                  @click="emit('resolveLocation', item)"
                >
                  定位“{{ item.placeName }}”
                </button>
              </div>
            </div>
          </div>

          <article class="item-card" :class="{ 'has-image': itemImage(item) }">
            <div v-if="itemImage(item)" class="item-image">
              <img
                :src="itemImage(item)!"
                :alt="item.placeName"
                referrerpolicy="no-referrer"
                @error="failedImages[item.id] = true"
              />
            </div>

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

              <div v-if="hasCoordinates(item)" class="location-result">
                <div class="location-status">
                  <strong><span aria-hidden="true">✓</span> 地点已定位</strong>
                  <small>现在可以用于本日相邻节点的路线计算</small>
                </div>
                <div class="location-copy">
                  <b>{{ matchedName(item) }}</b>
                  <span>{{ matchedAddress(item) }}</span>
                  <details>
                    <summary>查看坐标</summary>
                    <code>{{ matchedLongitude(item) }}, {{ matchedLatitude(item) }}</code>
                  </details>
                </div>
                <a :href="amapUrl(item)" target="_blank" rel="noreferrer">在高德地图查看 ↗</a>
              </div>
            </div>

            <div class="item-actions">
              <button
                type="button"
                :disabled="!canResolve(item) || busyAction === `location-${item.id}`"
                @click="emit('resolveLocation', item)"
              >
                {{
                  busyAction === `location-${item.id}`
                    ? '定位中…'
                    : hasCoordinates(item)
                      ? '重新定位'
                      : '定位地点'
                }}
              </button>
              <button type="button" @click="emit('edit', item)">编辑</button>
              <button type="button" class="danger" @click="emit('remove', item)">删除</button>
            </div>
          </article>
        </template>
      </section>
    </div>
  </section>
</template>

<style scoped>
.timeline-card {
  padding: 24px;
  border: 1px solid #eadfd9;
  border-radius: 20px;
  background: #fffaf6;
  box-shadow: var(--shadow-soft);
}
header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
header span {
  color: var(--coral);
  font-size: 12px;
}
h2 {
  margin: 5px 0 0;
  font-size: 22px;
}
.add-button {
  padding: 10px 15px;
  border: 0;
  border-radius: 20px;
  background: var(--coral-strong);
  color: #fff;
  cursor: pointer;
}
.empty {
  padding: 48px 10px;
  color: var(--text-muted);
  text-align: center;
}
.empty p {
  margin: 8px 0 0;
  font-size: 13px;
}
.day-list {
  display: grid;
  margin-top: 24px;
  gap: 24px;
}
.day-block {
  display: grid;
  gap: 10px;
}
.day-heading {
  display: flex;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee1da;
  align-items: center;
  justify-content: space-between;
}
.day-heading strong {
  font-size: 18px;
}
.day-heading span,
.day-empty {
  color: var(--text-muted);
  font-size: 12px;
}
.day-empty {
  padding: 16px 0;
  border: 1px dashed #eadfd9;
  border-radius: 12px;
  text-align: center;
}
.item-card {
  display: grid;
  padding: 17px;
  border: 1px solid #eee2dc;
  border-radius: 15px;
  background: #fff;
  grid-template-columns: 145px minmax(0, 1fr) auto;
  gap: 18px;
}
.item-card.has-image {
  grid-template-columns: 112px 125px minmax(0, 1fr) auto;
}
.item-image {
  overflow: hidden;
  width: 112px;
  height: 92px;
  border-radius: 10px;
  background: #f1e7e1;
  align-self: start;
}
.item-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.time-column {
  display: grid;
  align-content: start;
  gap: 5px;
}
.time-column strong {
  font-size: 13px;
}
.time-column span {
  color: var(--text-muted);
  font-size: 12px;
}
.item-title {
  display: flex;
  align-items: center;
  gap: 9px;
}
.item-title span {
  padding: 3px 7px;
  border-radius: 10px;
  background: #f9e5df;
  color: #a95e5e;
  font-size: 11px;
}
.item-title h3 {
  margin: 0;
  font-size: 17px;
}
.item-main > p {
  margin: 8px 0;
  color: #6d615c;
  font-size: 13px;
  line-height: 1.7;
}
.location-result {
  display: grid;
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #cfe2d2;
  border-radius: 12px;
  background: #f1f8f2;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}
.location-status {
  display: grid;
  gap: 3px;
}
.location-status strong {
  color: #356342;
  font-size: 12px;
}
.location-status strong span {
  display: inline-grid;
  width: 18px;
  height: 18px;
  margin-right: 3px;
  border-radius: 50%;
  background: #4d805c;
  color: #fff;
  place-items: center;
}
.location-status small {
  max-width: 190px;
  color: #6d8a74;
  font-size: 11px;
  line-height: 1.4;
}
.location-copy {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.location-copy b {
  font-size: 13px;
}
.location-copy > span {
  overflow: hidden;
  color: #596d5e;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.location-copy details {
  color: #6f8474;
  font-size: 10px;
}
.location-copy summary {
  cursor: pointer;
}
.location-copy code {
  font-family: ui-monospace, monospace;
  font-size: 10px;
}
.location-result a {
  color: #3f704c;
  font-size: 11px;
  text-decoration: none;
  white-space: nowrap;
}
.item-actions {
  display: flex;
  max-width: 185px;
  align-content: start;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 7px;
}
.item-actions button,
.route-controls button,
.route-controls select,
.locate-controls button {
  min-height: 34px;
  padding: 7px 10px;
  border: 1px solid #e6d9d2;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}
.item-actions button:disabled,
.route-controls button:disabled,
.locate-controls button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.danger {
  color: #ae575b;
}
.route-connector {
  display: grid;
  min-height: 82px;
  margin: -4px 18px;
  padding: 10px 14px;
  border: 1px solid #e5d9d2;
  border-radius: 14px;
  background: #f8f3ee;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
}
.route-mark {
  display: grid;
  position: relative;
  place-items: center;
}
.route-mark:before {
  position: absolute;
  width: 1px;
  height: 100%;
  background: #d7c7bf;
  content: '';
}
.route-mark span {
  position: relative;
  display: grid;
  width: 25px;
  height: 25px;
  border: 1px solid #d9c7be;
  border-radius: 50%;
  background: #fff;
  color: #9d6b62;
  place-items: center;
}
.route-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
}
.route-copy {
  display: grid;
  gap: 4px;
}
.route-copy strong {
  font-size: 13px;
}
.route-copy span {
  color: #74665f;
  font-size: 12px;
  line-height: 1.5;
}
.route-copy .distance-preview {
  color: #8b6255;
  font-weight: 600;
}
.route-copy .route-result {
  color: #497052;
}
.route-controls,
.locate-controls {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 7px;
}
.route-controls label {
  align-self: center;
  color: #74665f;
  font-size: 12px;
}
.route-controls select {
  min-width: 116px;
}
@media (max-width: 1050px) {
  .item-card {
    grid-template-columns: 125px minmax(0, 1fr) auto;
  }
  .item-card.has-image {
    grid-template-columns: 104px 112px minmax(0, 1fr);
  }
  .item-card.has-image .item-actions {
    grid-column: 2/-1;
  }
  .item-image {
    width: 104px;
    height: 84px;
  }
  .route-content {
    align-items: flex-start;
    flex-direction: column;
  }
  .route-controls,
  .locate-controls {
    justify-content: flex-start;
  }
}
@media (max-width: 760px) {
  .item-card {
    grid-template-columns: 1fr;
  }
  .item-card.has-image {
    grid-template-columns: 96px minmax(0, 1fr);
  }
  .item-card.has-image .item-image {
    grid-column: 1;
    grid-row: 1;
  }
  .item-card.has-image .time-column {
    grid-column: 2;
    grid-row: 1;
  }
  .item-card.has-image .item-main,
  .item-card.has-image .item-actions {
    grid-column: 1/-1;
  }
  .item-image {
    width: 96px;
    height: 78px;
  }
  .item-actions {
    max-width: none;
    justify-content: flex-start;
  }
  .location-result {
    grid-template-columns: 1fr;
  }
  .location-copy > span {
    white-space: normal;
  }
  .route-connector {
    margin: -3px 5px;
  }
  .route-controls {
    width: 100%;
  }
  .route-controls select,
  .route-controls button {
    flex: 1;
  }
}
@media (max-width: 560px) {
  .timeline-card {
    padding: 18px;
  }
  .item-card {
    padding: 14px;
  }
  .route-content {
    gap: 10px;
  }
  .locate-controls button {
    width: 100%;
  }
}
</style>
