import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import CityExploreView from '@/views/CityExploreView.vue'
import PlanPreparationView from '@/views/PlanPreparationView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import MyPlansView from '@/views/MyPlansView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AdminView from '@/views/AdminView.vue'
import { useAuthStore } from '@/stores/auth'
import { getSafeRedirect } from '@/utils/navigation'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/explore/:city(xiamen|chengdu|suzhou)',
      name: 'city-explore',
      component: CityExploreView,
    },
    {
      path: '/plan/new',
      name: 'plan-preparation',
      component: PlanPreparationView,
      meta: { requiresAuth: true },
    },
    {
      path: '/plans',
      name: 'plans',
      component: MyPlansView,
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guestOnly: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }

    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.initialize()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return { name: 'home' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return getSafeRedirect(to.query.redirect)
  }

  return true
})

export default router
