<script setup lang="ts">
import type { Attraction } from '@/types/city'

defineProps<{
  favorites: Attraction[]
}>()

const emit = defineEmits<{
  remove: [id: string]
  plan: []
}>()
</script>

<template>
  <section class="favorite-tray" aria-live="polite">
    <div class="tray-title">
      <span>♥</span>
      <div>
        <strong>已收藏 {{ favorites.length }} 个地点</strong>
        <small>{{
          favorites.length ? '带着心动地点进入行程规划' : '点亮景点卡片上的爱心吧'
        }}</small>
      </div>
    </div>

    <div class="favorite-list">
      <button
        v-for="attraction in favorites"
        :key="attraction.id"
        type="button"
        :title="`取消收藏 ${attraction.name}`"
        @click="emit('remove', attraction.id)"
      >
        <img :src="attraction.image" :alt="attraction.name" />
        <span>{{ attraction.name }}</span>
        <b aria-hidden="true">×</b>
      </button>
    </div>

    <button
      class="plan-button"
      type="button"
      :disabled="favorites.length === 0"
      @click="emit('plan')"
    >
      带上这些地点去规划 →
    </button>
  </section>
</template>

<style scoped>
.favorite-tray {
  position: fixed;
  z-index: 6;
  right: 24px;
  bottom: 20px;
  left: 202px;
  display: grid;
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid rgba(232, 205, 194, 0.9);
  border-radius: 18px;
  background: rgba(255, 251, 247, 0.94);
  box-shadow: 0 15px 45px rgba(67, 47, 39, 0.16);
  backdrop-filter: blur(18px);
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
}

.tray-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tray-title > span {
  display: grid;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--coral-light);
  color: var(--coral);
  place-items: center;
}

.tray-title div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.tray-title strong {
  font-size: 11px;
}

.tray-title small {
  color: var(--text-muted);
  font-size: 8px;
}

.favorite-list {
  display: flex;
  min-width: 0;
  overflow-x: auto;
  gap: 7px;
}

.favorite-list button {
  display: flex;
  min-width: max-content;
  height: 42px;
  padding: 4px 8px 4px 4px;
  border: 1px solid #ece1db;
  border-radius: 21px;
  background: white;
  color: #5f544f;
  cursor: pointer;
  align-items: center;
  gap: 6px;
  font-size: 9px;
}

.favorite-list img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.favorite-list b {
  color: #ad9e97;
  font-size: 13px;
}

.plan-button {
  padding: 12px 17px;
  border: 0;
  border-radius: 22px;
  background: var(--coral-strong);
  color: white;
  cursor: pointer;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.plan-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (max-width: 900px) {
  .favorite-tray {
    left: 24px;
  }
}

@media (max-width: 640px) {
  .favorite-tray {
    right: 10px;
    bottom: 10px;
    left: 10px;
    grid-template-columns: 1fr auto;
    gap: 8px;
  }

  .favorite-list {
    display: none;
  }

  .plan-button {
    grid-column: 2;
    grid-row: 1;
    max-width: 145px;
    padding: 11px 13px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .tray-title small {
    display: none;
  }
}
</style>
