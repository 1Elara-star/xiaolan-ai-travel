<script setup lang="ts">
import type { Attraction } from '@/types/city'

defineProps<{
  attractions: Attraction[]
  selectedId: string
  favorites: string[]
}>()

const emit = defineEmits<{
  select: [id: string]
  toggleFavorite: [id: string]
}>()
</script>

<template>
  <section class="attraction-section">
    <header>
      <div>
        <span>01</span>
        <h2>看看哪些地方让你心动</h2>
      </div>
      <p>故事、特色和出行提醒，都放在这里。</p>
    </header>

    <div class="attraction-grid">
      <article
        v-for="attraction in attractions"
        :key="attraction.id"
        class="attraction-card"
        :class="{ selected: selectedId === attraction.id }"
        tabindex="0"
        @click="emit('select', attraction.id)"
        @keydown.enter="emit('select', attraction.id)"
      >
        <img :src="attraction.image" :alt="attraction.name" />
        <div class="card-content">
          <div class="title-row">
            <div>
              <h3>{{ attraction.name }}</h3>
              <span>{{ attraction.subtitle }}</span>
            </div>
            <button
              type="button"
              :aria-label="`${favorites.includes(attraction.id) ? '取消收藏' : '收藏'}${attraction.name}`"
              @click.stop="emit('toggleFavorite', attraction.id)"
            >
              {{ favorites.includes(attraction.id) ? '♥' : '♡' }}
            </button>
          </div>
          <dl>
            <div>
              <dt>景点故事</dt>
              <dd>{{ attraction.story }}</dd>
            </div>
            <div>
              <dt>吸引大家的原因</dt>
              <dd>{{ attraction.popularReason }}</dd>
            </div>
          </dl>
          <div class="tags">
            <span v-for="tag in attraction.tags" :key="tag">{{ tag }}</span>
          </div>
          <footer>
            <span>建议停留：{{ attraction.suggestedDuration }}</span
            ><b>{{ favorites.includes(attraction.id) ? '已收藏' : '收藏' }}</b>
          </footer>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.attraction-section {
  padding: 34px 0 120px;
}

header {
  display: flex;
  margin-bottom: 17px;
  align-items: flex-end;
  justify-content: space-between;
}

header > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

header > div > span {
  color: var(--coral);
  font-size: 12px;
}

h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 25px;
}

header p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.attraction-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.attraction-card {
  display: grid;
  min-height: 285px;
  overflow: hidden;
  border: 1px solid #eadfd9;
  border-radius: var(--radius-medium);
  background: #fffdfa;
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  grid-template-columns: 40% 1fr;
  transition: 160ms ease;
}

.attraction-card:hover,
.attraction-card.selected {
  border-color: #f2aaa7;
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.attraction-card > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  display: flex;
  min-width: 0;
  padding: 16px;
  flex-direction: column;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 20px;
}

.title-row div > span {
  color: var(--text-muted);
  font-size: 12px;
}

.title-row button {
  border: 0;
  background: transparent;
  color: var(--coral-strong);
  cursor: pointer;
  font-size: 20px;
}

dl {
  display: grid;
  margin: 13px 0;
  gap: 10px;
}

dt {
  margin-bottom: 3px;
  font-size: 12px;
  font-weight: 700;
}

dd {
  margin: 0;
  color: #766a65;
  display: -webkit-box;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tags {
  display: flex;
  margin-top: auto;
  gap: 5px;
  flex-wrap: wrap;
}

.tags span {
  padding: 4px 7px;
  border-radius: 10px;
  background: #f6f0eb;
  color: #81736d;
  font-size: 11px;
}

footer {
  display: flex;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0e7e2;
  color: #8c7e78;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
}

footer b {
  color: var(--coral);
}

@media (max-width: 1050px) {
  .attraction-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  header {
    align-items: flex-start;
    flex-direction: column;
    gap: 7px;
  }

  .attraction-card {
    grid-template-columns: 1fr;
  }

  .attraction-card > img {
    height: 190px;
  }
}
</style>
