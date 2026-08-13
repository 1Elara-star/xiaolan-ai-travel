import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import { AUTH_TOKEN_KEY, AUTH_UNAUTHORIZED_EVENT, AUTH_USER_KEY } from '@/constants/auth'
import type {
  AuthUser,
  LoginRequest,
  RegisterRequest,
  UserInfoResponse,
} from '@/types/auth'

function readStoredUser(): AuthUser | null {
  const value = localStorage.getItem(AUTH_USER_KEY)
  if (!value) return null

  try {
    return JSON.parse(value) as AuthUser
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(AUTH_TOKEN_KEY))
  const user = ref<AuthUser | null>(readStoredUser())
  const initialized = ref(false)
  let initializationPromise: Promise<void> | null = null
  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const displayName = computed(() => user.value?.nickname?.trim() || user.value?.username || '登录')

  function persistUser(nextUser: AuthUser) {
    user.value = nextUser
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
  }

  function applyUserInfo(info: UserInfoResponse) {
    persistUser({
      userId: info.id,
      username: info.username,
      nickname: info.nickname,
      role: info.role,
    })
  }

  async function signIn(request: LoginRequest) {
    const response = await authApi.login(request)
    token.value = response.token
    persistUser({
      userId: response.userId,
      username: response.username,
      nickname: response.nickname,
      role: response.role,
    })
    localStorage.setItem(AUTH_TOKEN_KEY, response.token)
    initialized.value = true
  }

  async function signUp(request: RegisterRequest) {
    return authApi.register(request)
  }

  function signOut() {
    token.value = null
    user.value = null
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
  }

  function syncClearedSession() {
    token.value = null
    user.value = null
    initialized.value = true
  }

  async function initialize() {
    if (initialized.value) return
    if (initializationPromise) return initializationPromise

    initializationPromise = (async () => {
      const storedToken = localStorage.getItem(AUTH_TOKEN_KEY)
      if (!storedToken) {
        syncClearedSession()
        return
      }

      token.value = storedToken
      try {
        applyUserInfo(await authApi.getCurrentUser())
      } catch {
        // A 401 is cleared by the shared HTTP interceptor. For a temporary network
        // failure, keep the cached session so public browsing is not interrupted.
        if (!localStorage.getItem(AUTH_TOKEN_KEY)) syncClearedSession()
      } finally {
        initialized.value = true
      }
    })()

    try {
      await initializationPromise
    } finally {
      initializationPromise = null
    }
  }

  window.addEventListener(AUTH_UNAUTHORIZED_EVENT, syncClearedSession)

  return {
    token,
    user,
    initialized,
    isAuthenticated,
    displayName,
    initialize,
    applyUserInfo,
    signIn,
    signUp,
    signOut,
  }
})
