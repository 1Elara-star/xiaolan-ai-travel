<script setup lang="ts">
import { citySlugs, cities } from '@/data/cities'
import type { CityExploreData, CitySlug } from '@/types/city'

defineProps<{
  city: CityExploreData
  activeCategory: string
}>()

const emit = defineEmits<{
  changeCity: [slug: CitySlug]
  changeCategory: [category: string]
}>()
</script>

<template>
  <header class="city-header">
    <div class="city-copy">
      <p>城市灵感</p>
      <h1>{{ city.slogan }}</h1>
      <span>{{ city.description }}</span>

      <label class="city-select">
        <span>选择城市</span>
        <select
          :value="city.slug"
          aria-label="切换探索城市"
          @change="emit('changeCity', ($event.target as HTMLSelectElement).value as CitySlug)"
        >
          <option v-for="slug in citySlugs" :key="slug" :value="slug">
            {{ cities[slug].name }}
          </option>
        </select>
      </label>

      <div class="city-facts">
        <span><b>建议时长</b>{{ city.recommendedDays }}</span>
        <span><b>适合季节</b>{{ city.bestSeason }}</span>
      </div>
    </div>

    <figure class="city-postcard">
      <span class="tape"></span>
      <img :src="city.heroImage" :alt="`${city.name}城市风景`" />
      <figcaption>{{ city.name }}旅行一景</figcaption>
    </figure>

    <nav class="category-tabs" aria-label="景点分类">
      <button
        v-for="category in city.categories"
        :key="category"
        type="button"
        :class="{ active: activeCategory === category }"
        @click="emit('changeCategory', category)"
      >
        {{ category }}
      </button>
    </nav>
  </header>
</template>

<style scoped>
.city-header {
  position: relative;
  display: grid;
  padding: 54px 48px 30px;
  border: 1px solid var(--border-soft);
  border-radius: 0 0 22px 22px;
  background:
    radial-gradient(circle at 20% 20%, rgba(247, 177, 162, 0.13), transparent 36%), #fffaf6;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 30px;
  max-width: 100%;
  overflow: hidden;
}

.city-copy {
  min-width: 0;
}

.city-copy > p {
  margin: 0 0 12px;
  color: var(--coral);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h1 {
  max-width: 680px;
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(36px, 4vw, 55px);
  line-height: 1.18;
  overflow-wrap: anywhere;
}

.city-copy > span {
  display: block;
  max-width: 610px;
  margin-top: 14px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.75;
}

.city-select {
  display: flex;
  width: 260px;
  height: 44px;
  margin-top: 23px;
  padding: 0 14px;
  border: 1px solid #eaded7;
  border-radius: 22px;
  background: white;
  align-items: center;
  justify-content: space-between;
}

.city-select span {
  color: #9b8d87;
  font-size: 12px;
}

.city-select select {
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
  font-weight: 700;
}

.city-facts {
  display: flex;
  margin-top: 15px;
  gap: 24px;
}

.city-facts span {
  display: grid;
  color: #554b47;
  gap: 3px;
  font-size: 13px;
}

.city-facts b {
  color: #a2948d;
  font-size: 12px;
  font-weight: 500;
}

.city-postcard {
  position: relative;
  width: 275px;
  margin: 0 auto;
  padding: 11px 11px 29px;
  border: 1px solid #e6d9d2;
  background: white;
  box-shadow: var(--shadow-soft);
  transform: rotate(1.5deg);
}

.city-postcard img {
  width: 100%;
  height: 205px;
  object-fit: cover;
}

.city-postcard figcaption {
  margin-top: 8px;
  color: #766861;
  font-family: var(--font-display);
  text-align: center;
}

.tape {
  position: absolute;
  top: -11px;
  left: 98px;
  width: 82px;
  height: 21px;
  background: rgba(218, 180, 143, 0.44);
  transform: rotate(-3deg);
}

.category-tabs {
  display: flex;
  width: 100%;
  min-width: 0;
  padding-top: 4px;
  grid-column: 1 / -1;
  gap: 8px;
  overflow-x: auto;
}

.category-tabs button {
  padding: 8px 14px;
  border: 1px solid #eadfd9;
  border-radius: 18px;
  background: white;
  color: #6f625d;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.category-tabs button.active {
  border-color: var(--coral-light);
  background: var(--coral-light);
  color: #b9595d;
}

@media (max-width: 760px) {
  .city-header {
    min-width: 0;
    padding: 32px 20px 24px;
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: clamp(34px, 10vw, 46px);
  }

  .city-postcard {
    display: none;
  }
}
</style>
