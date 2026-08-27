<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type {
  TravelPlanDraft,
  TravelPlanDraftItem,
  TravelValidationIssue,
} from '@/types/travel'

const props = withDefaults(
  defineProps<{
    draft: TravelPlanDraft
    expiresAt: string | null
    validationIssues?: TravelValidationIssue[]
    hasErrors?: boolean
    enriching?: boolean
    adopting?: boolean
    regenerating?: boolean
  }>(),
  {
    validationIssues: () => [],
    hasErrors: false,
    enriching: false,
    adopting: false,
    regenerating: false,
  },
)

const emit = defineEmits<{
  enrich: []
  adopt: [draftItemKeys: string[]]
  regenerate: [additionalRequirements: string]
}>()

const selectedKeys = ref<string[]>([])
const failedImages = ref<string[]>([])
const additionalRequirements = ref('')

watch(
  () => props.draft.items.map((item) => item.draftItemKey),
  (validKeys) => {
    selectedKeys.value = selectedKeys.value.filter((key) => validKeys.includes(key))
  },
)

const days = computed(() => {
  const grouped = new Map<number, TravelPlanDraftItem[]>()
  for (const item of props.draft.items) {
    const items = grouped.get(item.dayNumber) ?? []
    items.push(item)
    grouped.set(item.dayNumber, items)
  }
  return [...grouped.entries()]
    .sort(([left], [right]) => left - right)
    .map(([dayNumber, items]) => ({
      dayNumber,
      items: [...items].sort((left, right) => left.itemOrder - right.itemOrder),
    }))
})

const expiresText = computed(() => {
  if (!props.expiresAt) return ''
  const date = new Date(props.expiresAt)
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

const estimatedMapQueries = computed(() => {
  const names = new Set<string>()
  for (const item of props.draft.items) {
    if (
      (item.itemType === 'ATTRACTION' || item.itemType === 'EVENT') &&
      (item.longitude == null || item.latitude == null)
    ) {
      names.add(item.placeName.trim().toLowerCase())
    }
  }
  return names.size
})

const canCalculateLocalDistance = computed(() => {
  const locatedByDay = new Map<number, number>()
  for (const item of props.draft.items) {
    if (item.longitude != null && item.latitude != null) {
      locatedByDay.set(item.dayNumber, (locatedByDay.get(item.dayNumber) ?? 0) + 1)
    }
  }
  return [...locatedByDay.values()].some((count) => count >= 2)
})

const canEnrich = computed(
  () => estimatedMapQueries.value > 0 || canCalculateLocalDistance.value,
)

const visibleIssues = computed(() => props.validationIssues.slice(0, 6))

function issueLocation(issue: TravelValidationIssue) {
  if (!issue.draftItemKey) return '整份行程'
  return issue.relatedDraftItemKey
    ? `${issue.relatedDraftItemKey} → ${issue.draftItemKey}`
    : issue.draftItemKey
}

function toggleItem(key: string) {
  selectedKeys.value = selectedKeys.value.includes(key)
    ? selectedKeys.value.filter((candidate) => candidate !== key)
    : [...selectedKeys.value, key]
}

function toggleDay(items: TravelPlanDraftItem[]) {
  const dayKeys = items.map((item) => item.draftItemKey)
  const allSelected = dayKeys.every((key) => selectedKeys.value.includes(key))
  selectedKeys.value = allSelected
    ? selectedKeys.value.filter((key) => !dayKeys.includes(key))
    : [...new Set([...selectedKeys.value, ...dayKeys])]
}

function formatTime(item: TravelPlanDraftItem) {
  if (!item.startTime && !item.endTime) return '时间待确认'
  const endSuffix = item.endDayOffset === 1 ? '（次日）' : ''
  return `${item.startTime || '待定'} — ${item.endTime || '待定'}${endSuffix}`
}

function formatDistance(distance: number) {
  return distance >= 1000 ? `${(distance / 1000).toFixed(1)} 公里` : `${distance} 米`
}

function splitTags(value: string | null) {
  return value
    ? value.split(/[，、,;；]/).map((tag) => tag.trim()).filter(Boolean).slice(0, 4)
    : []
}

function typeLabel(type: string) {
  return (
    {
      ATTRACTION: '景点',
      FOOD: '用餐',
      HOTEL: '住宿',
      EVENT: '活动',
      REST: '休息',
      OTHER: '其他',
    }[type] ?? type
  )
}

function sourceLabel(item: TravelPlanDraftItem) {
  if (item.sourceType === 'FAVORITE') return '来自我的收藏'
  if (item.sourceType === 'LOCAL_ATTRACTION') return '本地景点资料'
  return '小兰推荐'
}

function markImageFailed(key: string) {
  if (!failedImages.value.includes(key)) failedImages.value.push(key)
}
</script>

<template>
  <section class="draft-card">
    <header class="draft-header">
      <div>
        <p class="eyebrow">小兰给出的候选行程</p>
        <h2>{{ draft.destination }} · {{ draft.travelDays }} 天</h2>
        <p v-if="draft.summary" class="summary">{{ draft.summary }}</p>
      </div>
      <span v-if="expiresText" class="expires">临时保留至 {{ expiresText }}</span>
    </header>

    <section
      v-if="validationIssues.length"
      class="validation-panel"
      :class="{ error: hasErrors }"
      aria-live="polite"
    >
      <div class="validation-summary">
        <strong>
          {{ hasErrors ? '这份候选行程还有需要处理的问题' : '这份候选行程有一些提醒' }}
        </strong>
        <span>系统只负责检查，目前没有自动修改你的安排。</span>
      </div>
      <ul>
        <li v-for="issue in visibleIssues" :key="`${issue.code}-${issue.draftItemKey}-${issue.message}`">
          <span :class="['severity', issue.severity.toLowerCase()]">
            {{ issue.severity === 'ERROR' ? '需处理' : '提醒' }}
          </span>
          <span class="issue-location">{{ issueLocation(issue) }}</span>
          <span>{{ issue.message }}</span>
        </li>
      </ul>
      <small v-if="validationIssues.length > visibleIssues.length">
        还有 {{ validationIssues.length - visibleIssues.length }} 个问题未展开
      </small>
    </section>

    <div class="draft-actions">
      <div>
        <strong>先看懂，再选择</strong>
        <span>勾选满意的节点加入详细行程，未选节点不会保存。</span>
      </div>
      <div class="buttons">
        <button
          type="button"
          class="secondary"
          :disabled="!canEnrich || enriching || adopting"
          @click="emit('enrich')"
        >
          {{
            enriching
              ? '正在补全…'
              : estimatedMapQueries > 0
                ? `补全地点与距离（最多 ${estimatedMapQueries} 个地点）`
                : '计算已有地点的直线距离'
          }}
        </button>
        <button
          type="button"
          class="primary"
          :disabled="selectedKeys.length === 0 || adopting || enriching"
          @click="emit('adopt', selectedKeys)"
        >
          {{ adopting ? '正在加入…' : `将已选 ${selectedKeys.length} 个加入详细行程` }}
        </button>
      </div>
    </div>

    <p class="fact-note">
      图片与故事优先来自项目景点库；地图信息只有在你主动点击补全时才查询。直线距离不是实际步行或公交距离。
    </p>

    <div class="regenerate-box">
      <label for="additional-requirements">对这份候选还有什么要求？</label>
      <div>
        <textarea
          id="additional-requirements"
          v-model="additionalRequirements"
          maxlength="500"
          rows="2"
          placeholder="例如：第二天轻松一点，不去海边，多安排我收藏的人文景点。"
        />
        <button
          type="button"
          :disabled="!additionalRequirements.trim() || regenerating || enriching || adopting"
          @click="emit('regenerate', additionalRequirements.trim())"
        >
          {{ regenerating ? '正在重新生成…' : '按补充要求重新生成' }}
        </button>
      </div>
      <span>会生成一份新的候选方案，已经保存的详细行程不会改变。</span>
    </div>

    <div class="days">
      <section v-for="day in days" :key="day.dayNumber" class="day">
        <div class="day-heading">
          <h3>Day {{ day.dayNumber }}</h3>
          <button type="button" class="day-select" @click="toggleDay(day.items)">
            {{
              day.items.every((item) => selectedKeys.includes(item.draftItemKey))
                ? '取消当天'
                : '选择当天'
            }}
          </button>
        </div>

        <div class="candidate-list">
          <article
            v-for="item in day.items"
            :key="item.draftItemKey"
            class="candidate"
            :class="{ selected: selectedKeys.includes(item.draftItemKey) }"
          >
            <div
              v-if="item.imageUrl && !failedImages.includes(item.draftItemKey)"
              class="candidate-image"
            >
              <img
                :src="item.imageUrl"
                :alt="item.placeName"
                referrerpolicy="no-referrer"
                @error="markImageFailed(item.draftItemKey)"
              />
            </div>
            <div v-else-if="item.itemType === 'ATTRACTION'" class="image-placeholder">
              <span>定位地点后显示实景图</span>
            </div>

            <div class="candidate-content">
              <div class="meta-row">
                <span class="type">{{ typeLabel(item.itemType) }}</span>
                <span class="source" :class="item.sourceType?.toLowerCase()">
                  {{ sourceLabel(item) }}
                </span>
                <span class="time">{{ formatTime(item) }}</span>
              </div>

              <div class="title-row">
                <h4>{{ item.placeName }}</h4>
                <button
                  type="button"
                  class="select-button"
                  :aria-pressed="selectedKeys.includes(item.draftItemKey)"
                  @click="toggleItem(item.draftItemKey)"
                >
                  {{ selectedKeys.includes(item.draftItemKey) ? '已选择' : '选择这个节点' }}
                </button>
              </div>

              <p v-if="item.description" class="description">{{ item.description }}</p>
              <p v-if="item.featureDescription" class="feature">
                <strong>值得去：</strong>{{ item.featureDescription }}
              </p>
              <p v-if="item.storyBackground" class="story">
                <strong>这里的故事：</strong>{{ item.storyBackground }}
              </p>

              <div v-if="splitTags(item.suitableTags).length" class="tags">
                <span v-for="tag in splitTags(item.suitableTags)" :key="tag">{{ tag }}</span>
              </div>

              <div class="facts">
                <span v-if="item.suggestDuration">建议停留 {{ item.suggestDuration }} 分钟</span>
                <span v-if="item.matchedPoiName">已匹配：{{ item.matchedPoiName }}</span>
                <span v-if="item.address">{{ item.address }}</span>
                <span v-if="item.straightLineDistanceFromPrev != null">
                  与上一有效地点直线距离约
                  {{ formatDistance(item.straightLineDistanceFromPrev) }}
                </span>
                <span v-if="item.longitude == null || item.latitude == null" class="pending">
                  地图信息待补全
                </span>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.draft-card{display:grid;padding:28px;border:1px solid #eadfd9;border-radius:22px;background:#fffaf6;box-shadow:var(--shadow-card);gap:20px}.draft-header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px}.eyebrow{margin:0 0 6px;color:var(--coral);font-size:13px}.draft-header h2{margin:0;font-size:25px}.summary{max-width:760px;margin:10px 0 0;color:#756863;font-size:14px;line-height:1.75}.expires{flex:none;color:var(--text-muted);font-size:12px}.validation-panel{display:grid;padding:14px 16px;border:1px solid #e4d9bc;border-radius:14px;background:#fffaf0;gap:10px}.validation-panel.error{border-color:#e8c7c5;background:#fff3f2}.validation-summary{display:grid;gap:3px}.validation-summary strong{font-size:14px}.validation-summary span{color:#756863;font-size:12px}.validation-panel ul{display:grid;margin:0;padding:0;list-style:none;gap:7px}.validation-panel li{display:grid;align-items:center;grid-template-columns:auto auto minmax(0,1fr);gap:8px;color:#5f514c;font-size:13px;line-height:1.5}.severity{padding:2px 6px;border-radius:6px;font-size:11px}.severity.error{background:#f6d8d6;color:#994949}.severity.warning{background:#f3e7c8;color:#80652f}.issue-location{color:#96766b;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:11px}.validation-panel small{color:#887871}.draft-actions{display:flex;align-items:center;justify-content:space-between;padding:15px 17px;border:1px solid #eadfd9;border-radius:15px;background:#fff;gap:20px}.draft-actions>div:first-child{display:grid;gap:4px}.draft-actions strong{font-size:14px}.draft-actions span{color:#7b6c66;font-size:13px}.buttons{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:10px}.buttons button,.select-button,.day-select{border:0;cursor:pointer;font:inherit}.buttons button{padding:10px 15px;border-radius:11px;font-size:13px}.buttons button:disabled{cursor:not-allowed;opacity:.55}.secondary{border:1px solid #dfd0c9!important;background:#fff;color:#574943}.primary{background:var(--coral);color:#fff}.fact-note{margin:0;color:#8a7770;font-size:12px;line-height:1.6}.regenerate-box{display:grid;padding:15px 17px;border:1px solid #eadfd9;border-radius:15px;background:#fff;gap:8px}.regenerate-box label{font-size:14px;font-weight:600}.regenerate-box>div{display:flex;align-items:stretch;gap:10px}.regenerate-box textarea{min-height:58px;padding:10px 12px;border:1px solid #ded1ca;border-radius:10px;outline:none;resize:vertical;flex:1;font:inherit;font-size:13px;line-height:1.5}.regenerate-box textarea:focus{border-color:#e49793}.regenerate-box button{padding:0 16px;border:0;border-radius:10px;background:#574943;color:#fff;cursor:pointer;font-size:13px}.regenerate-box button:disabled{cursor:not-allowed;opacity:.5}.regenerate-box>span{color:#887871;font-size:12px}.days{display:grid;gap:24px}.day{display:grid;gap:12px}.day-heading{display:flex;align-items:center;justify-content:space-between;padding-bottom:9px;border-bottom:1px solid #eadfd9}.day-heading h3{margin:0;font-size:18px}.day-select{background:transparent;color:#a15b5a;font-size:12px}.candidate-list{display:grid;gap:12px}.candidate{display:grid;overflow:hidden;border:1px solid #e9ddd7;border-radius:16px;background:#fff;grid-template-columns:minmax(190px,30%) minmax(0,1fr);transition:border-color .2s,box-shadow .2s}.candidate.selected{border-color:#ef9b98;box-shadow:0 8px 22px rgba(178,105,96,.12)}.candidate-image,.image-placeholder{min-height:190px;background:#f2e9e3}.candidate-image img{display:block;width:100%;height:100%;min-height:190px;max-height:250px;object-fit:cover}.image-placeholder{display:grid;place-items:center;color:#9b8981;font-size:12px;background:linear-gradient(145deg,#f5ede7,#eee2db)}.candidate-content{display:grid;padding:18px 20px;align-content:start;gap:10px}.meta-row{display:flex;align-items:center;flex-wrap:wrap;gap:8px}.type,.source{padding:4px 8px;border-radius:7px;font-size:11px}.type{background:#fff0ed;color:#a65758}.source{background:#f3efe9;color:#75675f}.source.favorite{background:#fff2d9;color:#8c6633}.source.local_attraction{background:#edf5ed;color:#4d7453}.time{margin-left:auto;color:#74655f;font-size:12px}.title-row{display:flex;align-items:center;justify-content:space-between;gap:14px}.title-row h4{margin:0;color:#332724;font-size:20px}.select-button{flex:none;padding:8px 11px;border:1px solid #dfd0c9;border-radius:9px;background:#fff;color:#62534d;font-size:12px}.select-button[aria-pressed=true]{border-color:#ed8e8b;background:#fff0ed;color:#a54d50}.candidate-content p{margin:0;color:#70625c;font-size:13px;line-height:1.7}.candidate-content p strong{color:#51443f}.story{padding-left:11px;border-left:2px solid #ead3ca}.tags,.facts{display:flex;flex-wrap:wrap;gap:7px}.tags span{padding:4px 8px;border-radius:7px;background:#f7f1ec;color:#786861;font-size:11px}.facts{padding-top:9px;border-top:1px solid #f0e6e1}.facts span{color:#806f68;font-size:12px}.facts .pending{color:#a15b5a}@media(max-width:760px){.draft-card{padding:19px}.draft-header,.draft-actions{display:grid}.validation-panel li{grid-template-columns:auto minmax(0,1fr)}.validation-panel li>span:last-child{grid-column:1/-1}.buttons{justify-content:stretch}.buttons button{flex:1}.regenerate-box>div{display:grid}.regenerate-box button{min-height:42px}.candidate{grid-template-columns:1fr}.candidate-image,.image-placeholder{min-height:0;height:170px}.candidate-image img{min-height:0;height:170px}.candidate-content{padding:16px}.title-row{align-items:flex-start}.time{width:100%;margin-left:0}}
</style>
