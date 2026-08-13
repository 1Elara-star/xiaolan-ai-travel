<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import AuthFormField from '@/components/auth/AuthFormField.vue'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getResponseMessage } from '@/utils/apiError'
import { getSafeRedirect } from '@/utils/navigation'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const submitting = ref(false)

function validate() {
  if (username.value.trim().length < 3 || username.value.trim().length > 50)
    return '用户名长度需要在 3—50 个字符之间'
  if (password.value.length < 6 || password.value.length > 30)
    return '密码长度需要在 6—30 个字符之间'
  if (password.value !== confirmPassword.value) return '两次输入的密码不一致'
  return ''
}

function getRegisterError(error: unknown) {
  return getResponseMessage(error) || '注册失败，请检查后端是否正常运行'
}

async function submit() {
  errorMessage.value = validate()
  if (errorMessage.value) return

  submitting.value = true
  try {
    await authStore.signUp({
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim() || undefined,
    })
    await router.replace({
      name: 'login',
      query: {
        username: username.value.trim(),
        registered: 'true',
        redirect: getSafeRedirect(route.query.redirect),
      },
    })
  } catch (error) {
    errorMessage.value = getRegisterError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <header class="form-header">
      <p>第一次见面</p>
      <h1>创建账号</h1>
      <span>只保存你主动选择留下的旅行信息。</span>
    </header>
    <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
    <form @submit.prevent="submit">
      <AuthFormField
        id="register-username"
        v-model="username"
        label="用户名"
        autocomplete="username"
        placeholder="3—50 个字符"
        :maxlength="50"
      />
      <AuthFormField
        id="register-nickname"
        v-model="nickname"
        label="昵称（可选）"
        autocomplete="nickname"
        placeholder="小兰以后怎么称呼你"
        :maxlength="50"
      />
      <AuthFormField
        id="register-password"
        v-model="password"
        label="密码"
        :type="showPassword ? 'text' : 'password'"
        autocomplete="new-password"
        placeholder="6—30 个字符"
        :maxlength="30"
      >
        <button type="button" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </button>
      </AuthFormField>
      <AuthFormField
        id="register-confirm-password"
        v-model="confirmPassword"
        label="确认密码"
        :type="showPassword ? 'text' : 'password'"
        autocomplete="new-password"
        placeholder="再输入一次密码"
        :maxlength="30"
      />
      <button class="submit-button" type="submit" :disabled="submitting">
        {{ submitting ? '正在注册…' : '注册' }}
      </button>
    </form>
    <p class="switch-link">
      已经有账号？
      <RouterLink
        :to="{ name: 'login', query: { redirect: getSafeRedirect(route.query.redirect) } }"
      >
        直接登录
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
  margin-top: 25px;
  gap: 15px;
}
.error-message {
  margin: 18px 0 -8px;
  padding: 11px 13px;
  border-radius: 10px;
  background: #fff0ee;
  color: #b64f52;
  font-size: 13px;
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
  margin-top: 20px;
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
