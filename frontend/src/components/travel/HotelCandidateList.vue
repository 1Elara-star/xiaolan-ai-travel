<script setup lang="ts">
import type { HotelCandidate } from '@/types/travel'

defineProps<{
  hotels: HotelCandidate[]
  loaded: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  load: []
}>()
</script>

<template>
  <section class="hotel-card">
    <header>
      <div>
        <span>飞猪真实候选</span>
        <h2>酒店参考</h2>
      </div>
      <button type="button" :disabled="loading" @click="emit('load')">
        {{ loading ? '正在查询…' : loaded ? '重新查询' : '查询酒店' }}
      </button>
    </header>
    <p class="notice">仅在点击后调用飞猪；价格和库存以跳转后的平台页面为准。</p>

    <div v-if="loaded && hotels.length === 0" class="empty">没有查到酒店候选</div>
    <div v-else-if="hotels.length" class="hotel-list">
      <article v-for="hotel in hotels" :key="`${hotel.hotelName}-${hotel.detailUrl}`">
        <img v-if="hotel.imageUrl" :src="hotel.imageUrl" :alt="hotel.hotelName" />
        <div class="hotel-copy">
          <small>{{ hotel.brandName || hotel.source }}</small>
          <h3>{{ hotel.hotelName }}</h3>
          <p>{{ hotel.address || '地址以平台详情为准' }}</p>
          <div>
            <strong>{{ hotel.price || '价格待查询' }}</strong>
            <span v-if="hotel.star">{{ hotel.star }}</span>
          </div>
        </div>
        <a
          v-if="hotel.detailUrl"
          :href="hotel.detailUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          查看平台详情
        </a>
      </article>
    </div>
  </section>
</template>

<style scoped>
.hotel-card{padding:24px;border:1px solid #eadfd9;border-radius:20px;background:#fffaf6;box-shadow:var(--shadow-soft)}header{display:flex;align-items:flex-end;justify-content:space-between}header span{color:var(--coral);font-size:12px}h2{margin:5px 0 0;font-size:22px}header button{padding:9px 14px;border:1px solid #e7d9d2;border-radius:18px;background:#fff;cursor:pointer}header button:disabled{opacity:.6}.notice{margin:12px 0 0;color:var(--text-muted);font-size:12px}.hotel-list{display:grid;margin-top:18px;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.hotel-list article{display:grid;overflow:hidden;border:1px solid #eee2dc;border-radius:14px;background:#fff;grid-template-columns:105px minmax(0,1fr);grid-template-rows:1fr auto}.hotel-list img{width:105px;height:100%;min-height:128px;object-fit:cover;grid-row:1/3}.hotel-copy{padding:14px}.hotel-copy small{color:var(--coral)}.hotel-copy h3{margin:4px 0 6px;font-size:15px}.hotel-copy p{margin:0;color:var(--text-muted);font-size:12px}.hotel-copy div{display:flex;margin-top:10px;align-items:center;justify-content:space-between}.hotel-copy span{color:var(--text-muted);font-size:11px}.hotel-list a{padding:0 14px 12px;color:#a95e5e;font-size:12px;text-decoration:none}.empty{padding:30px;color:var(--text-muted);text-align:center}
@media(max-width:720px){.hotel-list{grid-template-columns:1fr}}@media(max-width:440px){.hotel-list article{grid-template-columns:82px minmax(0,1fr)}.hotel-list img{width:82px}}
</style>
