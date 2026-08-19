<script setup lang="ts">
import type { InspirationCard } from '@/types/home'
import type { CitySlug } from '@/types/city'

defineProps<{
  cards: InspirationCard[]
  favorites: Record<string, boolean>
}>()

const emit = defineEmits<{
  selectCity: [slug: CitySlug]
  toggleFavorite: [city: string]
}>()
</script>

<template>
  <section id="inspiration" class="inspiration-section">
    <div class="section-heading">
      <div>
        <span>01</span>
        <h2>为你推荐</h2>
      </div>
      <button type="button" disabled title="更多城市数据接入后开放">更多城市即将加入</button>
    </div>

    <div class="inspiration-grid">
      <article v-for="card in cards" :key="card.city" class="destination-card">
        <img :src="card.image" :alt="`${card.city}旅行风景`" />
        <div class="card-shade"></div>
        <div class="destination-info">
          <h3>{{ card.city }}</h3>
          <p>{{ card.duration }} · {{ card.theme }}</p>
          <span>{{ card.description }}</span>
        </div>
        <div class="card-actions">
          <button
            type="button"
            :aria-label="`${favorites[card.city] ? '取消收藏' : '收藏'}${card.city}`"
            @click="emit('toggleFavorite', card.city)"
          >
            {{ favorites[card.city] ? '♥ 已收藏' : '♡ 收藏' }}
          </button>
          <button type="button" @click="emit('selectCity', card.slug)">去看看 →</button>
        </div>
      </article>

      <article class="more-card">
        <span class="paper-tape"></span>
        <div>
          <h3>更多灵感</h3>
          <p>去发现心动的下一站</p>
        </div>
        <div class="mini-photos">
          <img src="/images/inspiration-coast.jpg" alt="海边旅行灵感" />
          <img src="/images/inspiration-nature.jpg" alt="自然旅行灵感" />
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.inspiration-section {
  padding-top: 16px;
}

.section-heading {
  display: flex;
  margin-bottom: 14px;
  align-items: center;
  justify-content: space-between;
}

.section-heading div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-heading div span {
  color: var(--coral);
  font-size: 12px;
}

.section-heading h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 19px;
}

.section-heading > button {
  border: 0;
  background: transparent;
  color: #9b8a82;
  cursor: pointer;
  font-size: 12px;
}

.section-heading > button:disabled {
  cursor: default;
}

.inspiration-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) 0.82fr;
  gap: 12px;
}

.destination-card {
  position: relative;
  min-height: 230px;
  overflow: hidden;
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-card);
}

.destination-card > img,
.card-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.destination-card > img {
  object-fit: cover;
  transition: transform 450ms ease;
}

.destination-card:hover > img {
  transform: scale(1.04);
}

.card-shade {
  background: linear-gradient(180deg, transparent 28%, rgba(21, 18, 17, 0.77) 100%);
}

.destination-info,
.card-actions {
  position: absolute;
  z-index: 1;
  right: 15px;
  left: 15px;
  color: white;
}

.destination-info {
  bottom: 45px;
}

.destination-info h3 {
  margin: 0 0 3px;
  font-family: var(--font-display);
  font-size: 26px;
}

.destination-info p {
  margin: 0 0 5px;
  font-size: 13px;
  font-weight: 700;
}

.destination-info span {
  display: block;
  max-width: 95%;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  line-height: 1.5;
}

.card-actions {
  bottom: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  color: white;
  cursor: pointer;
  font-size: 12px;
}

.more-card {
  position: relative;
  display: flex;
  min-height: 230px;
  padding: 22px 18px 15px;
  border: 1px solid #eaded6;
  border-radius: var(--radius-medium);
  background: #fffaf5;
  box-shadow: var(--shadow-card);
  flex-direction: column;
  justify-content: space-between;
}

.paper-tape {
  position: absolute;
  top: -8px;
  right: 7px;
  width: 58px;
  height: 17px;
  background: rgba(218, 180, 143, 0.44);
  transform: rotate(15deg);
}

.more-card h3 {
  margin: 0 0 5px;
  font-family: var(--font-display);
  font-size: 21px;
}

.more-card p {
  margin: 0;
  color: #93837b;
  font-size: 9px;
}

.mini-photos {
  display: flex;
  padding: 0 5px 5px;
  align-items: flex-end;
  justify-content: center;
}

.mini-photos img {
  width: 68px;
  height: 88px;
  padding: 4px 4px 14px;
  border: 1px solid #e5d9d2;
  background: white;
  box-shadow: 0 7px 14px rgba(60, 43, 36, 0.11);
  object-fit: cover;
}

.mini-photos img:first-child {
  transform: rotate(-8deg);
}

.mini-photos img:last-child {
  margin-left: -12px;
  transform: rotate(7deg);
}

@media (max-width: 900px) {
  .inspiration-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .inspiration-grid {
    grid-template-columns: 1fr;
  }

  .destination-card,
  .more-card {
    min-height: 260px;
  }
}
</style>
