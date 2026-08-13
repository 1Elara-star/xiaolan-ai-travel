<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import AuthFormField from '@/components/auth/AuthFormField.vue'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getResponseMessage } from '@/utils/apiError'
import { getSafeRedirect } from '@/utils/navigation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const username = ref(String(route.query.username ?? ''))
const password = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const submitting = ref(false)
const registered = computed(() => route.query.registered === 'true')

function validate() {
  if (username.value.trim().length < 3 || username.value.trim().length > 50) {
    return '用户名长度需要在 3—50 个字符之间'
  }
  if (password.value.length < 6 || password.value.length > 30) {
    return '密码长度需要在 6—30 个字符之间'
  }
  return ''
}

function getLoginError(error: unknown) {
  return getResponseMessage(error) || '登录失败，请检查用户名、密码和后端运行状态'
}

async function submit() {
  errorMessage.value = validate()
  if (errorMessage.value) return

  submitting.value = true
  try {
    await authStore.signIn({ username: username.value.trim(), password: password.value })
    await router.replace(
      authStore.user?.role === 'ADMIN'
        ? { name: 'admin' }
        : getSafeRedirect(route.query.redirect),
    )
  } catch (error) {
    errorMessage.value = getLoginError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <header class="form-header">
      <p>欢迎回来</p>
      <h1>登录小兰</h1>
      <span>继续整理你的行程和旅行偏好。</span>
    </header>

    <p v-if="registered" class="success-message">注册成功，现在可以登录了。</p>
    <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>

    <form @submit.prevent="submit">
      <AuthFormField
        id="login-username"
        v-model="username"
        label="用户名"
        autocomplete="username"
        placeholder="请输入用户名"
        :maxlength="50"
      />
      <AuthFormField
        id="login-password"
        v-model="password"
        label="密码"
        :type="showPassword ? 'text' : 'password'"
        autocomplete="current-password"
        placeholder="请输入密码"
        :maxlength="30"
      >
        <button type="button" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </button>
      </AuthFormField>

      <button class="submit-button" type="submit" :disabled="submitting">
        {{ submitting ? '正在登录…' : '登录' }}
      </button>
    </form>

    <p class="switch-link">
      还没有账号？
      <RouterLink
        :to="{ name: 'register', query: { redirect: getSafeRedirect(route.query.redirect) } }"
      >
        注册一个
      </RouterLink>
    </p>
  </AuthLayout>
</template>

<style scoped>
.form-header p {
  margin: 0 0 8px;
  color: var(--coral);
  font-size: 13px;
  font-weight: 700;
}
.form-header h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 40px;
}
.form-header span {
  display: block;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 14px;
}
form {
  display: grid;
  margin-top: 30px;
  gap: 20px;
}
.error-message,
.success-message {
  margin: 20px 0 -10px;
  padding: 11px 13px;
  border-radius: 10px;
  font-size: 13px;
}
.error-message {
  background: #fff0ee;
  color: #b64f52;
}
.success-message {
  background: #eef8f0;
  color: #4f805a;
}
.submit-button {
  height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 12px;
  background: var(--coral-strong);
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.submit-button:disabled {
  cursor: wait;
  opacity: 0.65;
}
.switch-link {
  margin-top: 22px;
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
}
.switch-link a {
  color: var(--coral);
  font-weight: 700;
  text-decoration: none;
}
</style>
