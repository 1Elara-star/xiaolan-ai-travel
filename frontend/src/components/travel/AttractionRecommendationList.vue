<script setup lang="ts">
import { reactive } from 'vue'

import type { AttractionRecommendation } from '@/types/travel'

defineProps<{
  recommendations: AttractionRecommendation[]
  travelDays: number
  loading?: boolean
  loaded?: boolean
  addingAttractionId?: number | null
}>()

const emit = defineEmits<{
  reload: []
  add: [recommendation: AttractionRecommendation, dayNumber: number]
}>()

const selectedDays = reactive<Record<number, number>>({})

function selectedDay(attractionId: number) {
  return selectedDays[attractionId] ?? 1
}

function updateDay(attractionId: number, event: Event) {
  selectedDays[attractionId] = Number((event.target as HTMLSelectElement).value)
}

function formatDistance(meters: number | null) {
  if (meters == null) return '加入行程后可计算位置便利度'
  if (meters < 1000) return `距现有行程最近约 ${meters} 米（直线）`
  return `距现有行程最近约 ${(meters / 1000).toFixed(1)} 公里（直线）`
}
</script>

<template>
  <section class="recommendation-panel">
    <header>
      <div>
        <p>结合你的画像、收藏和本次旅行</p>
        <h2>可能适合你的景点</h2>
        <span>匹配度是小兰的内部推荐结果，不是景点平台评分。</span>
      </div>
      <button type="button" class="reload" :disabled="loading" @click="emit('reload')">
        {{ loading ? '计算中…' : loaded ? '重新计算' : '查看推荐' }}
      </button>
    </header>

    <div v-if="loading" class="state">正在根据现有资料计算推荐…</div>
    <div v-else-if="loaded && !recommendations.length" class="state">
      当前目的地还没有可推荐的新景点，或者已有景点都已加入行程。
    </div>

    <div v-else-if="recommendations.length" class="recommendation-grid">
      <article
        v-for="item in recommendations"
        :key="item.attraction.id"
        class="recommendation-card"
      >
        <img
          v-if="item.attraction.image"
          :src="item.attraction.image"
          :alt="item.attraction.name"
        />
        <div v-else class="image-placeholder">暂无景点图片</div>

        <div class="card-content">
          <div class="title-row">
            <div>
              <small>{{ item.attraction.category || '旅行地点' }}</small>
              <h3>{{ item.attraction.name }}</h3>
            </div>
            <strong class="match">{{ item.matchPercentage }}%<span>匹配</span></strong>
          </div>

          <p class="description">
            {{ item.attraction.popularReason || item.attraction.subtitle }}
          </p>
          <p class="distance">{{ formatDistance(item.nearestPlanDistanceMeters) }}</p>

          <ul>
            <li v-for="reason in item.recommendationReasons" :key="reason">{{ reason }}</li>
          </ul>

          <div class="actions">
            <label>
              加入
              <select
                :value="selectedDay(item.attraction.id)"
                @change="updateDay(item.attraction.id, $event)"
              >
                <option v-for="day in travelDays" :key="day" :value="day">Day {{ day }}</option>
              </select>
            </label>
            <button
              type="button"
              :disabled="addingAttractionId === item.attraction.id"
              @click="emit('add', item, selectedDay(item.attraction.id))"
            >
              {{ addingAttractionId === item.attraction.id ? '加入中…' : '加入行程' }}
            </button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.recommendation-panel {
  display: grid;
  padding: 28px;
  border: 1px solid #eadfd9;
  border-radius: 22px;
  background: #fffaf6;
  box-shadow: var(--shadow-card);
  gap: 22px;
}
.recommendation-panel > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}
.recommendation-panel header p {
  margin: 0 0 6px;
  color: var(--coral);
  font-size: 13px;
}
.recommendation-panel h2 {
  margin: 0;
  color: #332724;
  font-size: 25px;
}
.recommendation-panel header span {
  display: block;
  margin-top: 8px;
  color: #806f68;
  font-size: 13px;
}
.reload {
  flex: none;
  padding: 10px 15px;
  border: 1px solid #dfd0c9;
  border-radius: 12px;
  background: #fff;
  color: #5f504a;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
}
.reload:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.state {
  padding: 32px;
  border: 1px dashed #e5d8d1;
  border-radius: 15px;
  color: #887871;
  text-align: center;
  font-size: 14px;
}
.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.recommendation-card {
  display: grid;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #eadfd9;
  border-radius: 17px;
  background: #fff;
  grid-template-columns: 170px minmax(0, 1fr);
}
.recommendation-card > img,
.image-placeholder {
  width: 100%;
  height: 100%;
  min-height: 245px;
  object-fit: cover;
  background: #f1e7e1;
}
.image-placeholder {
  display: grid;
  place-items: center;
  color: #9b8981;
  font-size: 12px;
}
.card-content {
  display: grid;
  min-width: 0;
  padding: 18px;
  align-content: start;
  gap: 11px;
}
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.title-row small {
  color: #a15b5a;
  font-size: 12px;
}
.title-row h3 {
  margin: 4px 0 0;
  color: #302420;
  font-size: 20px;
}
.match {
  display: grid;
  min-width: 60px;
  color: #bc5c5f;
  text-align: right;
  font-size: 22px;
  line-height: 1;
}
.match span {
  margin-top: 4px;
  color: #907d76;
  font-size: 11px;
  font-weight: 400;
}
.description,
.distance {
  margin: 0;
  color: #71625c;
  font-size: 13px;
  line-height: 1.65;
}
.distance {
  color: #52705a;
}
.card-content ul {
  display: grid;
  margin: 0;
  padding: 10px 0 0 17px;
  border-top: 1px solid #f0e6e1;
  color: #776761;
  font-size: 12px;
  line-height: 1.6;
  gap: 3px;
}
.actions {
  display: flex;
  margin-top: auto;
  padding-top: 4px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.actions label {
  display: flex;
  align-items: center;
  color: #76655f;
  font-size: 12px;
  gap: 6px;
}
.actions select {
  padding: 8px;
  border: 1px solid #e2d5ce;
  border-radius: 9px;
  background: #fff;
  color: #554741;
}
.actions button {
  padding: 9px 14px;
  border: 0;
  border-radius: 18px;
  background: var(--coral-strong);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}
.actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
@media (max-width: 1050px) {
  .recommendation-grid {
    grid-template-columns: 1fr;
  }
  .recommendation-card {
    grid-template-columns: 190px minmax(0, 1fr);
  }
}
@media (max-width: 620px) {
  .recommendation-panel {
    padding: 19px;
  }
  .recommendation-panel > header {
    display: grid;
  }
  .reload {
    width: 100%;
  }
  .recommendation-card {
    grid-template-columns: 1fr;
  }
  .recommendation-card > img,
  .image-placeholder {
    height: 165px;
    min-height: 165px;
  }
  .actions {
    align-items: stretch;
    flex-direction: column;
  }
  .actions label {
    justify-content: space-between;
  }
  .actions button {
    width: 100%;
  }
}
</style>
