<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const { displayName, isAuthenticated } = storeToRefs(authStore)
const route = useRoute()

withDefaults(
  defineProps<{
    active?: 'home' | 'explore' | 'plans' | 'profile'
  }>(),
  { active: 'home' },
)
</script>

<template>
  <aside class="sidebar">
    <RouterLink class="brand" to="/" aria-label="小兰 AI Travel 首页">
      <span class="brand-cn">小兰</span>
      <span class="brand-en">AI Travel</span>
      <span class="brand-note">Have a nice trip!</span>
    </RouterLink>

    <nav class="primary-nav" aria-label="主要导航">
      <RouterLink class="nav-item" :class="{ active: active === 'home' }" to="/">
        <span aria-hidden="true">⌂</span>首页
      </RouterLink>
      <RouterLink class="nav-item" :class="{ active: active === 'explore' }" to="/explore/xiamen">
        <span aria-hidden="true">◇</span>城市探索
      </RouterLink>
      <RouterLink class="nav-item" to="/#inspiration">
        <span aria-hidden="true">✦</span>灵感发现
      </RouterLink>
      <RouterLink class="nav-item" :class="{ active: active === 'plans' }" to="/plans">
        <span aria-hidden="true">▦</span>我的行程
      </RouterLink>
      <RouterLink
        class="nav-item"
        :class="{ active: active === 'profile' }"
        :to="
          isAuthenticated
            ? { name: 'profile' }
            : { name: 'login', query: { redirect: route.fullPath } }
        "
      >
        <span aria-hidden="true">♙</span>{{ isAuthenticated ? displayName : '登录' }}
      </RouterLink>
    </nav>

    <div class="assistant-note">
      <strong>小兰 AI 旅行助手</strong>
      <span>正在陪你发现好地方</span>
    </div>
    <img
      class="robot"
      src="/images/xiaolan-traveler.png"
      alt="戴着贝雷帽、背着旅行包的小兰机器人"
    />
  </aside>
</template>

<style scoped>
.sidebar {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: 100vh;
  padding: 30px 20px 18px;
  border-right: 1px solid var(--border-soft);
  background: rgba(255, 251, 247, 0.78);
  flex-direction: column;
  backdrop-filter: blur(12px);
}

.brand {
  position: relative;
  display: flex;
  margin: 0 10px 36px;
  color: var(--text-main);
  align-items: baseline;
  flex-wrap: wrap;
  gap: 2px 7px;
  text-decoration: none;
}

.brand-cn {
  white-space: nowrap;
  font-family: var(--font-display);
  font-size: 31px;
  font-weight: 800;
  letter-spacing: -0.08em;
}

.brand-en {
  color: var(--coral);
  font-family: 'Segoe Script', cursive;
  font-size: 12px;
  transform: rotate(-4deg);
}

.brand-note {
  width: 100%;
  margin: -5px 0 0 54px;
  color: #a99388;
  font-family: 'Segoe Print', cursive;
  font-size: 7px;
  transform: rotate(-7deg);
}

.primary-nav {
  display: grid;
  gap: 8px;
}

.nav-item {
  display: flex;
  width: 100%;
  height: 45px;
  padding: 0 15px;
  border: 0;
  border-radius: var(--radius-small);
  background: transparent;
  color: #433b38;
  cursor: pointer;
  align-items: center;
  gap: 13px;
  font-size: 13px;
  text-align: left;
  text-decoration: none;
  transition: background 160ms ease;
}

.nav-item span {
  display: grid;
  width: 20px;
  font-size: 20px;
  place-items: center;
}

.nav-item:hover,
.nav-item.active {
  background: var(--coral-light);
}

.nav-item:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.nav-item:disabled:hover {
  background: transparent;
}

.assistant-note {
  display: grid;
  margin-top: auto;
  padding: 13px 10px;
  border: 1px solid #efe3dc;
  border-radius: 8px;
  background: rgba(255, 253, 249, 0.94);
  box-shadow: var(--shadow-soft);
  gap: 4px;
  transform: rotate(-2deg);
}

.assistant-note strong {
  font-size: 11px;
}

.assistant-note span {
  color: var(--text-muted);
  font-size: 9px;
}

.robot {
  width: 150px;
  margin: -5px auto -15px;
  mix-blend-mode: multiply;
}

@media (max-width: 900px) {
  .sidebar {
    position: sticky;
    top: 0;
    min-height: auto;
    padding: 12px 18px;
    border-right: 0;
    border-bottom: 1px solid var(--border-soft);
    flex-direction: row;
    align-items: center;
  }

  .brand {
    margin: 0;
    flex-wrap: nowrap;
  }

  .brand-note,
  .assistant-note,
  .robot {
    display: none;
  }

  .primary-nav {
    display: flex;
    margin-left: auto;
  }

  .nav-item {
    width: auto;
    height: 38px;
    padding: 0 10px;
    font-size: 0;
  }
}

@media (max-width: 640px) {
  .brand-cn {
    font-size: 24px;
  }

  .brand-en,
  .primary-nav .nav-item:nth-child(n + 3) {
    display: none;
  }
}
</style>
