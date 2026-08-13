import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import './assets/main.css'
import router from './router'
import { AUTH_UNAUTHORIZED_EVENT } from './constants/auth'
import { useAuthStore } from './stores/auth'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
const authStore = useAuthStore(pinia)

window.addEventListener(AUTH_UNAUTHORIZED_EVENT, () => {
  const currentRoute = router.currentRoute.value
  if (currentRoute.meta.requiresAuth && currentRoute.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: currentRoute.fullPath } })
  }
})

app.use(router)
void authStore.initialize()

app.mount('#app')
