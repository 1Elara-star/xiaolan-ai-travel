<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type { HotelCandidate, HotelLocationType, HotelSearchFilters } from '@/types/travel'

type SortMode = 'recommended' | 'trip' | 'profile' | 'budget' | 'comfort'

const props = defineProps<{
  hotels: HotelCandidate[]
  loaded: boolean
  loading: boolean
  parsing: boolean
  selectedHotelName?: string | null
  locationOptions: Partial<Record<HotelLocationType, string[]>>
  parsedFilters?: HotelSearchFilters | null
}>()

const emit = defineEmits<{
  load: [filters: HotelSearchFilters]
  parsePreference: [preference: string]
  select: [hotel: HotelCandidate]
}>()

const sortMode = ref<SortMode>('recommended')
const naturalPreference = ref('')
const locationType = ref<HotelLocationType>('BUSINESS_AREA')
const locationKeyword = ref('')
const minPrice = ref('')
const maxPrice = ref('')
const filterError = ref('')
const failedImages = ref<string[]>([])

const locationTypeLabels: Record<HotelLocationType, string> = {
  BUSINESS_AREA: '商圈',
  TRANSPORT_HUB: '交通枢纽',
  METRO_STATION: '地铁站',
  SCENIC_AREA: '景区附近',
  LANDMARK: '地标附近',
  ADMINISTRATIVE_AREA: '行政区域',
  CUSTOM: '自定义地点',
}

const currentLocationOptions = computed(() => props.locationOptions[locationType.value] ?? [])

watch(
  () => props.parsedFilters,
  (filters) => {
    if (!filters) return
    locationType.value = filters.locationType ?? 'CUSTOM'
    locationKeyword.value = filters.locationKeyword ?? ''
    minPrice.value = filters.minPrice?.toString() ?? ''
    maxPrice.value = filters.maxPrice?.toString() ?? ''
    filterError.value = ''
  },
  { deep: true },
)

function hotelPrice(hotel: HotelCandidate) {
  if (hotel.priceValue != null) return hotel.priceValue
  const match = hotel.price?.replaceAll(',', '').match(/\d+(?:\.\d+)?/)
  return match ? Number(match[0]) : null
}

function comfortScore(hotel: HotelCandidate) {
  const star = hotel.star ?? ''
  if (/五星|豪华/.test(star)) return 5
  if (/四星|高档/.test(star)) return 4
  if (/三星|舒适/.test(star)) return 3
  if (/二星|经济/.test(star)) return 2
  return 1
}

function numberOrUndefined(value: string) {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function searchHotels() {
  const minimum = numberOrUndefined(minPrice.value)
  const maximum = numberOrUndefined(maxPrice.value)
  if ((minimum != null && minimum < 0) || (maximum != null && maximum < 0)) {
    filterError.value = '价格不能小于 0。'
    return
  }
  if (minimum != null && maximum != null && minimum > maximum) {
    filterError.value = '最低价不能高于最高价。'
    return
  }

  filterError.value = ''
  emit('load', {
    locationType: locationKeyword.value.trim() ? locationType.value : undefined,
    locationKeyword: locationKeyword.value.trim() || undefined,
    minPrice: minimum,
    maxPrice: maximum,
  })
}

function parseNaturalPreference() {
  const preference = naturalPreference.value.trim()
  if (!preference) {
    filterError.value = '请先说说你希望住在哪里、预算大概是多少。'
    return
  }
  filterError.value = ''
  emit('parsePreference', preference)
}

const recommendedHotels = computed(() =>
  [...props.hotels].sort((left, right) => right.overallMatchScore - left.overallMatchScore),
)
const tripHotels = computed(() =>
  [...props.hotels].sort((left, right) => right.tripMatchScore - left.tripMatchScore),
)
const profileHotels = computed(() =>
  [...props.hotels].sort((left, right) => right.profileMatchScore - left.profileMatchScore),
)
const budgetHotels = computed(() =>
  [...props.hotels].sort(
    (left, right) =>
      (hotelPrice(left) ?? Number.MAX_SAFE_INTEGER) -
      (hotelPrice(right) ?? Number.MAX_SAFE_INTEGER),
  ),
)
const comfortHotels = computed(() =>
  [...props.hotels].sort((left, right) => {
    const difference = comfortScore(right) - comfortScore(left)
    return difference || (hotelPrice(left) ?? Infinity) - (hotelPrice(right) ?? Infinity)
  }),
)

const displayedHotels = computed(() => {
  if (sortMode.value === 'trip') return tripHotels.value
  if (sortMode.value === 'profile') return profileHotels.value
  if (sortMode.value === 'budget') return budgetHotels.value
  if (sortMode.value === 'comfort') return comfortHotels.value
  return recommendedHotels.value
})

const profileUsed = computed(() => props.hotels.some((hotel) => hotel.profileUsed))

const choices = computed(() => {
  if (!props.hotels.length) return []
  const used = new Set<HotelCandidate>()
  const pickDistinct = (hotels: HotelCandidate[]) => {
    const hotel = hotels.find((candidate) => !used.has(candidate)) ?? hotels[0]
    if (hotel) used.add(hotel)
    return hotel
  }
  const result: Array<{ mode: SortMode; eyebrow: string; hotel: HotelCandidate; reason: string }> =
    []
  const trip = pickDistinct(tripHotels.value)
  if (trip) {
    result.push({
      mode: 'trip',
      eyebrow: '更符合本次行程',
      hotel: trip,
      reason: trip.recommendationReasons[0] ?? '根据本次目的地与筛选条件排序',
    })
  }
  if (profileUsed.value) {
    const profile = pickDistinct(profileHotels.value)
    if (profile) {
      result.push({
        mode: 'profile',
        eyebrow: '更符合你的偏好',
        hotel: profile,
        reason:
          profile.recommendationReasons.find((reason) => reason.includes('偏好')) ??
          '结合你的预算与住宿偏好排序',
      })
    }
  }
  const budget = pickDistinct(budgetHotels.value)
  if (budget) {
    result.push({
      mode: 'budget',
      eyebrow: '预算友好',
      hotel: budget,
      reason:
        hotelPrice(budget) == null ? '优先查看低价候选' : `本批价格较低，约 ¥${hotelPrice(budget)}`,
    })
  }
  return result.slice(0, 3)
})

function advantages(hotel: HotelCandidate) {
  if (hotel.recommendationReasons.length) {
    return [...hotel.recommendationReasons]
      .sort((left, right) => Number(!left.includes('偏好')) - Number(!right.includes('偏好')))
      .slice(0, 3)
  }
  const reasons: string[] = []
  if (hotel.address) reasons.push('地址信息完整')
  if (hotel.latitude && hotel.longitude) reasons.push('坐标完整，可加入行程路线')
  if (hotel.star) reasons.push(`飞猪返回等级：${hotel.star}`)
  return reasons
}

function cardLabel(hotel: HotelCandidate) {
  if (props.selectedHotelName === hotel.hotelName) return '当前住宿'
  if (tripHotels.value[0] === hotel) return '本次行程优先'
  if (profileUsed.value && profileHotels.value[0] === hotel) return '偏好优先'
  if (budgetHotels.value[0] === hotel) return '本批低价'
  return null
}

function markImageFailed(hotel: HotelCandidate) {
  const key = `${hotel.hotelName}-${hotel.imageUrl}`
  if (!failedImages.value.includes(key)) failedImages.value.push(key)
}

function imageAvailable(hotel: HotelCandidate) {
  return Boolean(
    hotel.imageUrl && !failedImages.value.includes(`${hotel.hotelName}-${hotel.imageUrl}`),
  )
}
</script>

<template>
  <section class="hotel-card">
    <header>
      <div>
        <span>飞猪真实酒店 · 结合本次行程与个人偏好</span>
        <h2>酒店推荐</h2>
      </div>
    </header>

    <section class="preference-panel">
      <div>
        <strong>告诉小兰你想住在哪里</strong>
        <span>例如：想住在演唱会场馆附近，每晚不超过 500 元，去地铁站方便。</span>
      </div>
      <textarea
        v-model="naturalPreference"
        maxlength="500"
        rows="2"
        placeholder="输入你的住宿位置、价格和出行需求"
        @keydown.ctrl.enter.prevent="parseNaturalPreference"
      />
      <button type="button" :disabled="parsing" @click="parseNaturalPreference">
        {{ parsing ? '正在整理…' : '整理成筛选条件' }}
      </button>
    </section>

    <form class="filter-panel" @submit.prevent="searchHotels">
      <label class="location-type-filter">
        <span>住宿位置类型</span>
        <select v-model="locationType">
          <option v-for="(label, value) in locationTypeLabels" :key="value" :value="value">
            {{ label }}
          </option>
        </select>
      </label>
      <label class="location-filter">
        <span>具体位置</span>
        <input
          v-model="locationKeyword"
          list="hotel-location-options"
          :placeholder="`请选择或输入${locationTypeLabels[locationType]}`"
        />
        <datalist id="hotel-location-options">
          <option v-for="location in currentLocationOptions" :key="location" :value="location" />
        </datalist>
      </label>
      <label
        ><span>最低价</span><input v-model="minPrice" type="number" min="0" placeholder="不限"
      /></label>
      <span class="separator">—</span>
      <label
        ><span>最高价</span><input v-model="maxPrice" type="number" min="0" placeholder="不限"
      /></label>
      <button type="submit" :disabled="loading">
        {{ loading ? '正在查询…' : loaded ? '按条件重新查询' : '查询酒店' }}
      </button>
      <p v-if="filterError" class="filter-error">{{ filterError }}</p>
    </form>

    <p class="filter-help">
      小兰整理出的条件可以继续修改；也可以跳过自然语言，直接选择位置类型并输入地点。
    </p>
    <p class="notice">
      推荐使用飞猪返回的真实价格、等级、地址、坐标和图片，并结合你的筛选条件与住宿画像排序；这是小兰的匹配顺序，不是酒店平台评分。价格和库存以飞猪详情页为准。
    </p>

    <div v-if="loaded && hotels.length === 0" class="empty">
      当前条件没有查到酒店，可以更换住宿位置或放宽价格范围。
    </div>

    <template v-else-if="hotels.length">
      <div class="choice-title">
        <strong>先看这几种选择</strong>
        <span v-if="!profileUsed">填写住宿或预算偏好后，会增加“更符合你的偏好”推荐。</span>
      </div>
      <div class="choice-list" :class="{ compact: choices.length < 3 }">
        <button
          v-for="choice in choices"
          :key="choice.mode"
          type="button"
          :class="{ active: sortMode === choice.mode }"
          @click="sortMode = choice.mode"
        >
          <small>{{ choice.eyebrow }}</small>
          <strong>{{ choice.hotel.hotelName }}</strong>
          <span>{{ choice.reason }}</span>
        </button>
      </div>

      <div class="sort-bar">
        <span>排序方式</span>
        <div>
          <button
            type="button"
            :class="{ active: sortMode === 'recommended' }"
            @click="sortMode = 'recommended'"
          >
            综合推荐
          </button>
          <button type="button" :class="{ active: sortMode === 'trip' }" @click="sortMode = 'trip'">
            本次行程
          </button>
          <button
            v-if="profileUsed"
            type="button"
            :class="{ active: sortMode === 'profile' }"
            @click="sortMode = 'profile'"
          >
            我的偏好
          </button>
          <button
            type="button"
            :class="{ active: sortMode === 'budget' }"
            @click="sortMode = 'budget'"
          >
            价格从低到高
          </button>
          <button
            type="button"
            :class="{ active: sortMode === 'comfort' }"
            @click="sortMode = 'comfort'"
          >
            住宿等级
          </button>
        </div>
      </div>

      <div class="hotel-list">
        <article
          v-for="hotel in displayedHotels"
          :key="`${hotel.hotelName}-${hotel.detailUrl}`"
          :class="{ selected: selectedHotelName === hotel.hotelName }"
        >
          <div class="hotel-image">
            <img
              v-if="imageAvailable(hotel)"
              :src="hotel.imageUrl!"
              :alt="hotel.hotelName"
              referrerpolicy="no-referrer"
              @error="markImageFailed(hotel)"
            />
            <div v-else class="image-placeholder">该酒店暂未返回可用图片</div>
            <b v-if="cardLabel(hotel)">{{ cardLabel(hotel) }}</b>
          </div>
          <div class="hotel-copy">
            <div class="hotel-meta">
              <small>{{ hotel.brandName || hotel.source }}</small
              ><span v-if="hotel.star">{{ hotel.star }}</span>
            </div>
            <h3>{{ hotel.hotelName }}</h3>
            <p class="address">{{ hotel.address || '地址以飞猪详情为准' }}</p>
            <div class="advantages">
              <em>推荐依据</em>
              <ul>
                <li v-for="advantage in advantages(hotel)" :key="advantage">{{ advantage }}</li>
              </ul>
            </div>
            <div class="price-row">
              <strong>{{ hotel.price || '价格待查询' }}</strong
              ><span>平台实时价可能变化</span>
            </div>
          </div>
          <div class="hotel-actions">
            <button
              type="button"
              :disabled="selectedHotelName === hotel.hotelName"
              @click="emit('select', hotel)"
            >
              {{ selectedHotelName === hotel.hotelName ? '已选为住宿' : '选这家' }}
            </button>
            <a
              v-if="hotel.detailUrl"
              :href="hotel.detailUrl"
              target="_blank"
              rel="noopener noreferrer"
              >去飞猪核对详情 →</a
            >
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.hotel-card {
  padding: 26px;
  border: 1px solid #eadfd9;
  border-radius: 22px;
  background: #fffaf6;
  box-shadow: var(--shadow-soft);
}
header span {
  color: var(--coral);
  font-size: 13px;
  letter-spacing: 0.03em;
}
h2 {
  margin: 6px 0 0;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.preference-panel {
  display: grid;
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #eadfd8;
  border-radius: 16px;
  background: #fff;
  grid-template-columns: minmax(210px, 0.8fr) minmax(300px, 1.5fr) auto;
  align-items: center;
  gap: 12px;
}
.preference-panel > div {
  display: grid;
  gap: 5px;
}
.preference-panel strong {
  font-size: 14px;
}
.preference-panel span {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}
.preference-panel textarea {
  min-height: 64px;
  padding: 10px 12px;
  resize: vertical;
}
.preference-panel button {
  min-height: 42px;
  padding: 9px 15px;
  border: 0;
  border-radius: 11px;
  background: #4f443f;
  color: #fff;
  cursor: pointer;
}
.preference-panel button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.filter-panel {
  display: grid;
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #eadfd8;
  border-radius: 16px;
  background: #fff;
  grid-template-columns:
    minmax(135px, 0.7fr) minmax(220px, 1.4fr) minmax(100px, 0.6fr) auto
    minmax(100px, 0.6fr) auto;
  align-items: end;
  gap: 10px;
}
.filter-panel label {
  display: grid;
  gap: 6px;
}
.filter-panel label span {
  color: #75665f;
  font-size: 12px;
}
.filter-panel input,
.filter-panel select,
.preference-panel textarea {
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #dfd1ca;
  border-radius: 10px;
  background: #fffdfb;
  color: #3f3531;
  font: inherit;
  font-size: 13px;
  outline: none;
}
.filter-panel input:focus,
.filter-panel select:focus,
.preference-panel textarea:focus {
  border-color: #dd9893;
  box-shadow: 0 0 0 3px rgba(221, 152, 147, 0.12);
}
.filter-panel > button {
  min-height: 40px;
  padding: 9px 16px;
  border: 0;
  border-radius: 11px;
  background: var(--coral-strong);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}
.filter-panel > button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.separator {
  padding-bottom: 12px;
  color: #aa9b94;
}
.filter-error {
  margin: 0;
  color: #ac4e52;
  font-size: 12px;
  grid-column: 1/-1;
}
.filter-help,
.notice {
  margin: 9px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}
.notice {
  max-width: 980px;
}
.empty {
  padding: 36px;
  color: var(--text-muted);
  text-align: center;
}
.choice-title {
  display: flex;
  margin-top: 24px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}
.choice-title strong {
  font-size: 14px;
}
.choice-title span {
  color: var(--text-muted);
  font-size: 12px;
}
.choice-list {
  display: grid;
  margin-top: 10px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.choice-list.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.choice-list button {
  display: grid;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid #eadfd8;
  border-radius: 15px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  gap: 5px;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    background 160ms ease;
}
.choice-list button:hover {
  transform: translateY(-2px);
  border-color: #dcbdb4;
}
.choice-list button.active {
  border-color: #e7a39d;
  background: #fff3f0;
  box-shadow: 0 7px 18px rgba(166, 90, 80, 0.08);
}
.choice-list small {
  color: #b75d5c;
  font-size: 12px;
}
.choice-list strong {
  overflow: hidden;
  color: #2e2826;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.choice-list span {
  color: #8a7971;
  font-size: 12px;
  line-height: 1.45;
}
.sort-bar {
  display: flex;
  margin-top: 22px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.sort-bar > span {
  color: #71645e;
  font-size: 12px;
}
.sort-bar > div {
  display: flex;
  padding: 3px;
  border-radius: 18px;
  background: #f3ebe6;
  gap: 2px;
}
.sort-bar button {
  padding: 7px 12px;
  border: 0;
  border-radius: 15px;
  background: transparent;
  color: #7b6d66;
  cursor: pointer;
  font-size: 12px;
}
.sort-bar button.active {
  background: #fff;
  color: #403633;
  box-shadow: 0 2px 8px rgba(70, 49, 40, 0.08);
}
.hotel-list {
  display: grid;
  margin-top: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
}
.hotel-list article {
  display: grid;
  overflow: hidden;
  border: 1px solid #eadfd9;
  border-radius: 16px;
  background: #fff;
  grid-template-columns: 132px minmax(0, 1fr);
  grid-template-rows: 1fr auto;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.hotel-list article:hover {
  border-color: #ddc6bd;
  box-shadow: 0 10px 24px rgba(74, 52, 43, 0.07);
}
.hotel-list article.selected {
  border-color: #7ba187;
  box-shadow: 0 0 0 2px rgba(82, 128, 96, 0.08);
}
.hotel-image {
  position: relative;
  min-height: 220px;
  grid-row: 1/3;
  background: #f1ebe7;
}
.hotel-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  padding: 14px;
  color: #998a83;
  font-size: 12px;
  text-align: center;
  place-items: center;
}
.hotel-image b {
  position: absolute;
  top: 10px;
  left: 9px;
  padding: 5px 8px;
  border-radius: 10px;
  background: rgba(255, 250, 247, 0.94);
  color: #a34e50;
  box-shadow: 0 3px 10px rgba(53, 39, 34, 0.12);
  font-size: 11px;
  font-weight: 600;
}
.hotel-copy {
  min-width: 0;
  padding: 14px 15px 10px;
}
.hotel-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.hotel-meta small {
  color: var(--coral);
  font-size: 12px;
}
.hotel-meta span {
  color: #8a7770;
  font-size: 12px;
}
.hotel-copy h3 {
  margin: 5px 0;
  color: #282321;
  font-size: 15px;
  line-height: 1.4;
}
.address {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}
.advantages {
  margin-top: 12px;
}
.advantages em {
  display: block;
  color: #6b5a53;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}
.advantages ul {
  display: flex;
  margin: 6px 0 0;
  padding: 0;
  flex-wrap: wrap;
  gap: 5px;
  list-style: none;
}
.advantages li {
  padding: 4px 7px;
  border-radius: 8px;
  background: #f7f1ec;
  color: #745e55;
  font-size: 11px;
  line-height: 1.35;
}
.price-row {
  display: flex;
  margin-top: 13px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}
.price-row strong {
  font-size: 18px;
}
.price-row span {
  color: #aa9a92;
  font-size: 11px;
}
.hotel-actions {
  display: flex;
  padding: 0 15px 14px;
  align-items: center;
  gap: 12px;
}
.hotel-actions button {
  padding: 8px 13px;
  border: 0;
  border-radius: 16px;
  background: var(--coral-strong);
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}
.hotel-actions button:disabled {
  background: #6f957b;
  cursor: default;
}
.hotel-actions a {
  color: #a45c59;
  font-size: 12px;
  text-decoration: none;
}
.hotel-actions a:hover {
  text-decoration: underline;
}
@media (max-width: 900px) {
  .preference-panel {
    grid-template-columns: 1fr auto;
  }
  .preference-panel > div {
    grid-column: 1/-1;
  }
  .filter-panel {
    grid-template-columns: minmax(140px, 0.7fr) minmax(220px, 1fr) repeat(3, auto);
  }
  .filter-panel > button {
    grid-column: 1/-1;
  }
  .choice-list,
  .choice-list.compact {
    grid-template-columns: 1fr;
  }
  .hotel-list {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 600px) {
  .hotel-card {
    padding: 18px;
  }
  .preference-panel {
    grid-template-columns: 1fr;
  }
  .preference-panel > div,
  .preference-panel button {
    grid-column: auto;
  }
  .filter-panel {
    grid-template-columns: 1fr 16px 1fr;
  }
  .location-type-filter,
  .location-filter,
  .filter-panel > button,
  .filter-error {
    grid-column: 1/-1;
  }
  .choice-title,
  .sort-bar {
    align-items: flex-start;
    flex-direction: column;
  }
  .sort-bar > div {
    max-width: 100%;
    overflow-x: auto;
  }
  .hotel-list article {
    grid-template-columns: 104px minmax(0, 1fr);
  }
  .hotel-image {
    min-height: 236px;
  }
  .price-row span {
    display: none;
  }
}
</style>
