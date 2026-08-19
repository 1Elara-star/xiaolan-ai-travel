<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DiscoveryStrip from '@/components/home/DiscoveryStrip.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import InspirationSection from '@/components/home/InspirationSection.vue'
import PlanningPanel from '@/components/home/PlanningPanel.vue'
import { inspirationCards } from '@/data/home'
import { HOME_FAVORITES_KEY, HOME_IDEA_KEY, readJsonStorage } from '@/constants/draft'
import { useAuthStore } from '@/stores/auth'
import type { CitySlug } from '@/types/city'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const travelIdea = ref(localStorage.getItem(HOME_IDEA_KEY) ?? '')
const activeTag = ref('海边')
const favorites = ref<Record<string, boolean>>(readJsonStorage(HOME_FAVORITES_KEY, {}))
const accountMenuOpen = ref(false)
const saveMessage = ref('')
const searchText = ref('')
const searchMessage = ref('')

const ideaSummary = computed(() => travelIdea.value.trim() || '还没有写下旅行想法')

function toggleFavorite(city: string) {
  favorites.value[city] = !favorites.value[city]
  localStorage.setItem(HOME_FAVORITES_KEY, JSON.stringify(favorites.value))
}

function saveTravelIdea() {
  const idea = travelIdea.value.trim()
  if (!idea) {
    saveMessage.value = '先写下一点旅行想法吧。'
    return
  }

  localStorage.setItem(HOME_IDEA_KEY, idea)
  travelIdea.value = idea
  saveMessage.value = '已保存在这台设备，登录后创建行程时也会带上。'
}

function selectCity(slug: CitySlug) {
  void router.push({ name: 'city-explore', params: { city: slug } })
}

function searchCity() {
  const keyword = searchText.value.trim().toLowerCase()
  const matches: Array<[CitySlug, string[]]> = [
    ['xiamen', ['厦门', 'xiamen']],
    ['chengdu', ['成都', 'chengdu']],
    ['suzhou', ['苏州', 'suzhou']],
  ]
  const matched = matches.find(([, keywords]) => keywords.some((item) => keyword.includes(item)))

  if (matched) {
    searchMessage.value = ''
    selectCity(matched[0])
    return
  }

  searchMessage.value = keyword ? '目前可以探索厦门、成都和苏州。' : '请输入想看的城市。'
}

function openAccount() {
  if (authStore.isAuthenticated) {
    accountMenuOpen.value = !accountMenuOpen.value
    return
  }

  void router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function signOut() {
  authStore.signOut()
  accountMenuOpen.value = false
}
</script>

<template>
  <div class="home-shell">
    <HomeSidebar />

    <main class="main-content">
      <header class="topbar">
        <form class="top-search" role="search" @submit.prevent="searchCity">
          <span aria-hidden="true">⌕</span>
          <input v-model="searchText" type="search" placeholder="搜索厦门、成都或苏州" />
        </form>
        <span v-if="searchMessage" class="search-message" role="status">{{ searchMessage }}</span>
        <button class="notification" type="button" disabled title="消息中心暂未开放" aria-label="消息中心暂未开放">♧</button>
        <div class="account-entry">
          <button
            class="avatar"
            type="button"
            :aria-label="authStore.isAuthenticated ? `${authStore.displayName}的账户菜单` : '登录'"
            :title="authStore.isAuthenticated ? authStore.displayName : '登录或注册'"
            @click="openAccount"
          >
            {{ authStore.isAuthenticated ? authStore.displayName.slice(0, 1) : '登' }}
          </button>
          <div v-if="authStore.isAuthenticated && accountMenuOpen" class="account-menu">
            <strong>{{ authStore.displayName }}</strong>
            <span>@{{ authStore.user?.username }}</span>
            <button type="button" @click="signOut">退出登录</button>
          </div>
        </div>
      </header>

      <HomeHero
        v-model:travel-idea="travelIdea"
        v-model:active-tag="activeTag"
        :save-message="saveMessage"
        @save="saveTravelIdea"
      />
      <InspirationSection
        :cards="inspirationCards"
        :favorites="favorites"
        @select-city="selectCity"
        @toggle-favorite="toggleFavorite"
      />
      <DiscoveryStrip />
    </main>

    <PlanningPanel :idea-summary="ideaSummary" @start-exploring="selectCity('xiamen')" />
  </div>
</template>

<style scoped>
.home-shell {
  display: grid;
  min-height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(rgba(116, 88, 73, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(116, 88, 73, 0.025) 1px, transparent 1px), #faf6f1;
  background-size: 28px 28px;
  grid-template-columns: 178px minmax(680px, 1fr) 350px;
}

.main-content {
  min-width: 0;
  padding: 0 clamp(28px, 3vw, 52px) 40px;
}

.topbar {
  display: flex;
  height: 84px;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.top-search {
  display: flex;
  width: min(330px, 48%);
  height: 38px;
  padding: 0 16px;
  border: 1px solid #ecdfd8;
  border-radius: 22px;
  background: rgba(255, 253, 249, 0.82);
  box-shadow: 0 6px 18px rgba(76, 58, 49, 0.04);
  align-items: center;
  gap: 9px;
}

.top-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #554b47;
  font-size: 11px;
}

.search-message {
  color: #9a6a64;
  font-size: 12px;
}

.notification:disabled {
  cursor: default;
  opacity: 0.45;
}

.notification,
.avatar {
  position: relative;
  display: grid;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  place-items: center;
}

.account-entry {
  position: relative;
}

.account-menu {
  position: absolute;
  z-index: 8;
  top: 43px;
  right: 0;
  display: grid;
  width: 165px;
  padding: 14px;
  border: 1px solid #eaded7;
  border-radius: 12px;
  background: #fffdfa;
  box-shadow: var(--shadow-card);
  gap: 5px;
}

.account-menu strong {
  font-size: 13px;
}

.account-menu span {
  color: var(--text-muted);
  font-size: 12px;
}

.account-menu button {
  margin-top: 8px;
  padding: 8px;
  border: 1px solid #eaded7;
  border-radius: 8px;
  background: white;
  color: #b65b5f;
  cursor: pointer;
  font-size: 12px;
}

.notification i {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 6px;
  height: 6px;
  border: 1px solid #fff;
  border-radius: 50%;
  background: var(--coral);
}

.avatar {
  background: linear-gradient(145deg, #f7b3aa, #e48688);
  box-shadow: inset 0 0 0 3px #fff;
  color: white;
  font-size: 12px;
}

@media (max-width: 1260px) {
  .home-shell {
    grid-template-columns: 160px minmax(620px, 1fr);
  }
}

@media (max-width: 900px) {
  .home-shell {
    display: block;
    overflow: visible;
  }
}

@media (max-width: 640px) {
  .main-content {
    padding: 0 16px 28px;
  }

  .topbar {
    height: 68px;
  }

  .top-search {
    width: 100%;
  }
}
</style>
