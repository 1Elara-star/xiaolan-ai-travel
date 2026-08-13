<script setup lang="ts">
import type { Attraction } from '@/types/city'

defineProps<{
  attraction: Attraction
  isFavorite: boolean
}>()

const emit = defineEmits<{
  toggleFavorite: [id: string]
}>()
</script>

<template>
  <aside class="story-panel">
    <img :src="attraction.image" :alt="attraction.name" />
    <div class="story-copy">
      <p>小兰的地点笔记</p>
      <h2>{{ attraction.name }}，值得这样逛</h2>
      <span>{{ attraction.story }}</span>

      <dl>
        <div>
          <dt>这里有什么特别</dt>
          <dd>{{ attraction.popularReason }}</dd>
        </div>
        <div>
          <dt>拍照提示</dt>
          <dd>{{ attraction.photoTip }}</dd>
        </div>
        <div>
          <dt>出发前注意</dt>
          <dd>{{ attraction.reminder }}</dd>
        </div>
      </dl>

      <button type="button" @click="emit('toggleFavorite', attraction.id)">
        {{ isFavorite ? '♥ 已收藏，点击取消' : '♡ 收藏这个地点' }}
      </button>
    </div>
  </aside>
</template>

<style scoped>
.story-panel {
  position: sticky;
  top: 22px;
  overflow: hidden;
  border: 1px solid #eadfd9;
  border-radius: var(--radius-large);
  background: #fffaf6;
  box-shadow: var(--shadow-card);
}

.story-panel > img {
  width: 100%;
  height: 210px;
  object-fit: cover;
}

.story-copy {
  padding: 24px;
}

.story-copy > p {
  margin: 0 0 7px;
  color: var(--coral);
  font-size: 12px;
  font-weight: 700;
}

h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 25px;
  line-height: 1.35;
}

.story-copy > span {
  display: block;
  margin-top: 12px;
  color: #756965;
  font-size: 13px;
  line-height: 1.75;
}

dl {
  display: grid;
  margin: 20px 0;
  gap: 13px;
}

dl div {
  padding: 12px;
  border-radius: 12px;
  background: white;
}

dt {
  margin-bottom: 5px;
  font-size: 12px;
  font-weight: 700;
}

dd {
  margin: 0;
  color: #7e716b;
  font-size: 12px;
  line-height: 1.65;
}

button {
  width: 100%;
  padding: 12px 16px;
  border: 0;
  border-radius: 22px;
  background: var(--coral-strong);
  box-shadow: 0 8px 16px rgba(240, 120, 122, 0.2);
  color: white;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1050px) {
  .story-panel {
    position: static;
  }
}
</style>
