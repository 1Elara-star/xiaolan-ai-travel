<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as authApi from '@/api/auth'
import * as profileApi from '@/api/profile'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/profile'
import { getResponseMessage } from '@/utils/apiError'

const authStore = useAuthStore()
const account = reactive({ nickname: '', avatar: '', phone: '', email: '' })
const profile = reactive<UserProfile>({
  mbti: null,
  travelPace: null,
  budgetPreference: null,
  transportPreference: null,
  interestTags: null,
  dislikeTags: null,
  specialNotes: null,
  companionPreference: null,
  foodPreference: null,
  mealStylePreference: null,
  restaurantPreference: null,
  accommodationPreference: null,
})
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const success = ref('')

onMounted(async () => {
  try {
    const [user, saved] = await Promise.all([authApi.getCurrentUser(), profileApi.getProfile()])
    Object.assign(account, {
      nickname: user.nickname ?? '',
      avatar: user.avatar ?? '',
      phone: user.phone ?? '',
      email: user.email ?? '',
    })
    Object.assign(profile, saved)
    authStore.applyUserInfo(user)
  } catch (error) {
    message.value = getResponseMessage(error) || '读取资料失败。'
  } finally {
    loading.value = false
  }
})

async function save() {
  saving.value = true
  message.value = ''
  success.value = ''
  profile.mbti = profile.mbti?.trim().toUpperCase() || null

  try {
    const user = await authApi.updateCurrentUser(account)
    authStore.applyUserInfo(user)
    Object.assign(profile, await profileApi.saveProfile(profile))
    success.value = '资料已经保存。'
  } catch (error) {
    message.value = getResponseMessage(error) || '保存失败。'
  } finally {
    saving.value = false
  }
}
</script>
<template><div class="page-shell"><HomeSidebar active="profile"/><main><header><p>关于你</p><h1>让小兰慢慢认识你</h1><span>这些偏好将来会作为生成行程的输入；现在只负责可靠保存，不调用 AI。</span></header><p v-if="loading">正在读取…</p><form v-else @submit.prevent="save"><section><h2>账户资料</h2><div class="fields"><label><span>昵称</span><input v-model.trim="account.nickname" maxlength="50"></label><label><span>邮箱</span><input v-model.trim="account.email" type="email" maxlength="100"></label><label><span>手机号</span><input v-model.trim="account.phone" maxlength="11" pattern="1[3-9][0-9]{9}"></label><label><span>头像地址</span><input v-model.trim="account.avatar" maxlength="255"></label></div></section><section><h2>旅行画像</h2><div class="fields"><label><span>MBTI</span><input v-model.trim="profile.mbti" maxlength="4" pattern="[EeIi][SsNn][TtFf][JjPp]" placeholder="例如 INFP"></label><label><span>旅行节奏</span><select v-model="profile.travelPace"><option :value="null">未选择</option><option>轻松</option><option>适中</option><option>紧凑</option></select></label><label><span>预算偏好</span><input v-model.trim="profile.budgetPreference" maxlength="30" placeholder="例如：舒适实惠"></label><label><span>交通偏好</span><input v-model.trim="profile.transportPreference" maxlength="30" placeholder="例如：公共交通优先"></label><label class="wide"><span>兴趣标签</span><textarea v-model.trim="profile.interestTags" maxlength="500" placeholder="美食、拍照、人文…"></textarea></label><label class="wide"><span>不喜欢的方式</span><textarea v-model.trim="profile.dislikeTags" maxlength="500" placeholder="例如：不喜欢早起和频繁换酒店"></textarea></label><label><span>同行偏好</span><input v-model.trim="profile.companionPreference" maxlength="100"></label><label><span>饮食偏好</span><input v-model.trim="profile.foodPreference" maxlength="500"></label><label><span>就餐方式</span><input v-model.trim="profile.mealStylePreference" maxlength="500"></label><label><span>餐厅偏好</span><input v-model.trim="profile.restaurantPreference" maxlength="500"></label><label class="wide"><span>住宿偏好</span><textarea v-model.trim="profile.accommodationPreference" maxlength="500"></textarea></label><label class="wide"><span>其他说明</span><textarea v-model.trim="profile.specialNotes" maxlength="500"></textarea></label></div></section><p v-if="message" class="error" role="alert">{{message}}</p><p v-if="success" class="success" role="status">{{success}}</p><button class="submit" :disabled="saving" type="submit">{{saving?'保存中…':'保存我的偏好'}}</button></form></main></div></template>
<style scoped>.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{width:min(900px,calc(100% - 40px));margin:0 auto;padding:48px 0}header p{margin:0;color:var(--coral);font-size:12px}h1{margin:8px 0;font-family:var(--font-display);font-size:42px}header span{color:var(--text-muted);font-size:13px}form{display:grid;margin-top:28px;gap:18px}section{padding:25px;border:1px solid #eadfd9;border-radius:18px;background:#fffaf6}h2{margin:0 0 18px;font-size:18px}.fields{display:grid;grid-template-columns:1fr 1fr;gap:16px}label{display:grid;gap:7px}label span{font-size:12px;color:#625651}.wide{grid-column:1/-1}input,select,textarea{padding:11px 13px;border:1px solid #e7dad3;border-radius:10px;background:white;outline:none}textarea{min-height:72px;resize:vertical}.submit{padding:14px;border:0;border-radius:22px;background:var(--coral-strong);color:white;cursor:pointer}.error{color:#b75458}.success{color:#4d7658}@media(max-width:900px){.page-shell{display:block}}@media(max-width:600px){.fields{grid-template-columns:1fr}.wide{grid-column:auto}}</style>
