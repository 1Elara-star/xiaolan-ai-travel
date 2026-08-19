<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import * as favoritesApi from '@/api/favorites'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { syncGuestFavoritesToAccount } from '@/services/favoriteSync'
import type { FavoriteAttractionResponse } from '@/types/city'
import { getResponseMessage } from '@/utils/apiError'

const favorites = ref<FavoriteAttractionResponse[]>([])
const loading = ref(true)
const message = ref('')

onMounted(async () => {
  try {
    const syncResult = await syncGuestFavoritesToAccount()
    favorites.value = await favoritesApi.listFavoriteAttractions()
    if (syncResult.syncedCount > 0) {
      message.value = `登录前选择的 ${syncResult.syncedCount} 个地点已同步到账号。`
    }
  } catch (error) {
    message.value = getResponseMessage(error) || '暂时无法读取收藏。'
  } finally {
    loading.value = false
  }
})

async function removeFavorite(item: FavoriteAttractionResponse) {
  if (!window.confirm(`确定取消收藏“${item.attraction.name}”吗？`)) return
  try {
    await favoritesApi.removeFavoriteAttraction(item.attraction.id)
    favorites.value = favorites.value.filter((favorite) => favorite.favoriteId !== item.favoriteId)
  } catch (error) {
    message.value = getResponseMessage(error) || '取消收藏失败。'
  }
}
</script>

<template>
  <div class="page-shell">
    <HomeSidebar active="favorites" />
    <main>
      <header>
        <p>心动地点</p>
        <h1>我的收藏</h1>
        <span>这里显示已经同步到账号的景点收藏。</span>
      </header>
      <p v-if="message" class="message" role="status">{{ message }}</p>
      <p v-if="loading" class="empty">正在读取…</p>
      <section v-else-if="favorites.length" class="favorite-grid">
        <article v-for="favorite in favorites" :key="favorite.favoriteId">
          <img
            v-if="favorite.attraction.image"
            :src="favorite.attraction.image"
            :alt="favorite.attraction.name"
          />
          <div>
            <small>{{ favorite.attraction.city }} · {{ favorite.attraction.category }}</small>
            <h2>{{ favorite.attraction.name }}</h2>
            <p>{{ favorite.attraction.subtitle || favorite.attraction.popularReason }}</p>
            <span v-if="favorite.attraction.suggestedDuration">
              建议游览 {{ favorite.attraction.suggestedDuration }}
            </span>
            <button type="button" @click="removeFavorite(favorite)">取消收藏</button>
          </div>
        </article>
      </section>
      <div v-else class="empty">
        <p>还没有同步到账号的收藏。</p>
        <RouterLink to="/explore/xiamen">去城市探索看看</RouterLink>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{width:min(1050px,calc(100% - 42px));margin:0 auto;padding:48px 0 70px}header p{margin:0;color:var(--coral);font-size:12px}header h1{margin:8px 0;font-family:var(--font-display);font-size:44px}header span{color:var(--text-muted);font-size:13px}.message{padding:11px 14px;border-radius:10px;background:#edf5ed;color:#416749}.favorite-grid{display:grid;margin-top:28px;grid-template-columns:repeat(2,minmax(0,1fr));gap:15px}.favorite-grid article{display:grid;overflow:hidden;border:1px solid #eadfd9;border-radius:17px;background:#fff;box-shadow:var(--shadow-soft);grid-template-columns:170px minmax(0,1fr)}.favorite-grid img{width:170px;height:100%;min-height:190px;object-fit:cover}.favorite-grid article>div{display:grid;padding:19px;align-content:start;gap:7px}.favorite-grid small,.favorite-grid span{color:var(--text-muted);font-size:12px}.favorite-grid h2{margin:2px 0;font-size:20px}.favorite-grid p{margin:0;color:#6d615c;font-size:13px;line-height:1.6}.favorite-grid button{justify-self:start;margin-top:8px;padding:0;border:0;background:transparent;color:#ad575b;cursor:pointer;font-size:12px}.empty{margin-top:45px;color:var(--text-muted);text-align:center}.empty a{display:inline-block;margin-top:7px;padding:10px 16px;border-radius:19px;background:var(--coral-strong);color:white;text-decoration:none}
@media(max-width:900px){.page-shell{display:block}}@media(max-width:760px){.favorite-grid{grid-template-columns:1fr}}@media(max-width:460px){.favorite-grid article{grid-template-columns:1fr}.favorite-grid img{width:100%;height:160px;min-height:0}}
</style>
