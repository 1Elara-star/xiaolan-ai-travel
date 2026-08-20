<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import * as adminApi from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import type { AdminAttraction, AdminOverview, AdminPlan, AdminUser } from '@/types/admin'
import { getResponseMessage } from '@/utils/apiError'

type Section = 'overview' | 'users' | 'plans' | 'attractions'

const authStore = useAuthStore()
const router = useRouter()
const active = ref<Section>('overview')
const overview = ref<AdminOverview | null>(null)
const users = ref<AdminUser[]>([])
const plans = ref<AdminPlan[]>([])
const attractions = ref<AdminAttraction[]>([])
const userKeyword = ref('')
const planKeyword = ref('')
const attractionKeyword = ref('')
const loading = ref(false)
const busyUserId = ref<number | null>(null)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const sectionTitle = computed(() => ({
  overview: ['系统概览', '看看小兰今天运行得怎么样'],
  users: ['用户管理', '查询账号并维护系统角色'],
  plans: ['行程监管', '了解用户创建的旅行计划'],
  attractions: ['景点数据', '检查系统已有的真实地点资料'],
})[active.value])

const metrics = computed(() => overview.value ? [
  { label: '注册用户', value: overview.value.userCount, note: `${overview.value.adminCount} 位管理员`, tone: 'rose' },
  { label: '旅行计划', value: overview.value.planCount, note: '用户创建的全部行程', tone: 'amber' },
  { label: '景点资料', value: overview.value.attractionCount, note: '可用于探索与行程匹配', tone: 'green' },
  { label: '收藏记录', value: overview.value.favoriteCount, note: `${overview.value.memoryCount} 条旅行记忆`, tone: 'blue' },
] : [])

onMounted(async () => {
  await Promise.all([loadOverview(), loadUsers()])
})

function notify(value: string, type: 'success' | 'error' = 'success') {
  message.value = value
  messageType.value = type
}

async function run(action: () => Promise<void>) {
  loading.value = true
  message.value = ''
  try {
    await action()
  } catch (error) {
    notify(getResponseMessage(error) || '请求失败，请稍后重试。', 'error')
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  await run(async () => { overview.value = await adminApi.getOverview() })
}

async function loadUsers() {
  await run(async () => { users.value = await adminApi.listUsers(userKeyword.value) })
}

async function loadPlans() {
  await run(async () => { plans.value = await adminApi.listPlans(planKeyword.value) })
}

async function loadAttractions() {
  await run(async () => { attractions.value = await adminApi.listAttractions(attractionKeyword.value) })
}

async function selectSection(section: Section) {
  active.value = section
  if (section === 'users' && users.value.length === 0) await loadUsers()
  if (section === 'plans' && plans.value.length === 0) await loadPlans()
  if (section === 'attractions' && attractions.value.length === 0) await loadAttractions()
}

async function changeRole(user: AdminUser) {
  const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const label = nextRole === 'ADMIN' ? '设为管理员' : '取消管理员身份'
  if (!window.confirm(`确定将账号“${user.username}”${label}吗？`)) return
  busyUserId.value = user.id
  try {
    const updated = await adminApi.updateUserRole(user.id, nextRole)
    users.value = users.value.map((item) => item.id === updated.id ? updated : item)
    await loadOverview()
    notify(`账号“${updated.username}”的角色已更新。`)
  } catch (error) {
    notify(getResponseMessage(error) || '角色更新失败。', 'error')
  } finally {
    busyUserId.value = null
  }
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value))
}

function planStatus(value: string) {
  return ({ PLANNING: '规划中', UPCOMING: '即将出发', ONGOING: '旅行中', FINISHED: '已结束' } as Record<string, string>)[value] || value
}

async function signOut() {
  authStore.signOut()
  await router.push('/')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">兰</span>
        <div><strong>小兰管理台</strong><small>XIAOLAN ADMIN</small></div>
      </div>

      <nav aria-label="管理员导航">
        <button :class="{ active: active === 'overview' }" @click="selectSection('overview')"><span>⌂</span>系统概览</button>
        <button :class="{ active: active === 'users' }" @click="selectSection('users')"><span>♙</span>用户管理</button>
        <button :class="{ active: active === 'plans' }" @click="selectSection('plans')"><span>▦</span>行程监管</button>
        <button :class="{ active: active === 'attractions' }" @click="selectSection('attractions')"><span>◇</span>景点数据</button>
      </nav>

      <div class="sidebar-bottom">
        <RouterLink to="/">← 返回用户端</RouterLink>
        <button class="sign-out" type="button" @click="signOut">退出登录</button>
      </div>
    </aside>

    <main>
      <header class="topbar">
        <div>
          <p>管理员工作台</p>
          <h1>{{ sectionTitle[0] }}</h1>
          <span>{{ sectionTitle[1] }}</span>
        </div>
        <div class="admin-account">
          <span>{{ authStore.displayName.slice(0, 1) }}</span>
          <div><strong>{{ authStore.displayName }}</strong><small>管理员</small></div>
        </div>
      </header>

      <p v-if="message" class="message" :class="messageType">{{ message }}</p>

      <template v-if="active === 'overview'">
        <section class="metric-grid">
          <article v-for="metric in metrics" :key="metric.label" :class="metric.tone">
            <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.note }}</small>
          </article>
        </section>
        <section class="overview-grid">
          <article class="welcome-card">
            <span>今日管理提示</span>
            <h2>欢迎回来，{{ authStore.displayName }}</h2>
            <p>这里展示数据库中的真实统计。账号权限修改会立即写入数据库，被调整的用户下次登录时生效。</p>
            <button type="button" @click="selectSection('users')">查看用户列表 →</button>
          </article>
          <article class="health-card">
            <div><span class="health-dot"></span><strong>后端服务正常</strong></div>
            <p>管理员 JWT 鉴权和数据库查询均已通过。</p>
            <dl>
              <div><dt>权限模型</dt><dd>USER / ADMIN</dd></div>
              <div><dt>当前范围</dt><dd>最近 100 条记录</dd></div>
              <div><dt>隐私保护</dt><dd>不返回用户密码</dd></div>
            </dl>
          </article>
        </section>
        <section class="recent-card">
          <div class="section-heading"><div><span>最近注册</span><h2>新用户</h2></div><button @click="selectSection('users')">全部用户</button></div>
          <div class="mini-users">
            <article v-for="user in users.slice(0, 5)" :key="user.id">
              <span class="mini-avatar">{{ (user.nickname || user.username).slice(0, 1) }}</span>
              <div><strong>{{ user.nickname || user.username }}</strong><small>@{{ user.username }}</small></div>
              <b :class="user.role.toLowerCase()">{{ user.role }}</b>
              <time>{{ formatDate(user.createTime) }}</time>
            </article>
          </div>
        </section>
      </template>

      <section v-else-if="active === 'users'" class="data-card">
        <form class="toolbar" @submit.prevent="loadUsers">
          <input v-model="userKeyword" placeholder="搜索用户名、昵称或邮箱">
          <button :disabled="loading" type="submit">{{ loading ? '查询中…' : '搜索' }}</button>
          <button class="ghost" type="button" @click="userKeyword = ''; loadUsers()">重置</button>
        </form>
        <div class="table-wrap">
          <table>
            <thead><tr><th>用户</th><th>联系方式</th><th>角色</th><th>注册时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td><div class="identity"><span>{{ (user.nickname || user.username).slice(0, 1) }}</span><div><strong>{{ user.nickname || '未设置昵称' }}</strong><small>@{{ user.username }} · ID {{ user.id }}</small></div></div></td>
                <td><strong class="plain">{{ user.email || '未填写邮箱' }}</strong><small>{{ user.phone || '未填写手机号' }}</small></td>
                <td><b class="role" :class="user.role.toLowerCase()">{{ user.role }}</b></td>
                <td>{{ formatDate(user.createTime) }}</td>
                <td><button class="text-action" :disabled="busyUserId === user.id || user.id === authStore.user?.userId" @click="changeRole(user)">{{ busyUserId === user.id ? '更新中…' : user.role === 'ADMIN' ? '取消管理员' : '设为管理员' }}</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="!users.length && !loading" class="empty">没有找到符合条件的用户。</p>
      </section>

      <section v-else-if="active === 'plans'" class="data-card">
        <form class="toolbar" @submit.prevent="loadPlans">
          <input v-model="planKeyword" placeholder="搜索行程名称、出发地或目的地">
          <button :disabled="loading" type="submit">{{ loading ? '查询中…' : '搜索' }}</button>
          <button class="ghost" type="button" @click="planKeyword = ''; loadPlans()">重置</button>
        </form>
        <div class="table-wrap"><table><thead><tr><th>行程</th><th>创建用户</th><th>日期</th><th>天数</th><th>状态</th></tr></thead><tbody>
          <tr v-for="plan in plans" :key="plan.id"><td><strong>{{ plan.title }}</strong><small>{{ plan.destination }} · ID {{ plan.id }}</small></td><td>@{{ plan.username }}</td><td>{{ plan.startDate }} — {{ plan.endDate }}</td><td>{{ plan.travelDays }} 天</td><td><b class="status">{{ planStatus(plan.tripStatus) }}</b></td></tr>
        </tbody></table></div><p v-if="!plans.length && !loading" class="empty">还没有符合条件的行程。</p>
      </section>

      <section v-else class="data-card">
        <form class="toolbar" @submit.prevent="loadAttractions">
          <input v-model="attractionKeyword" placeholder="搜索景点名称、城市或类型">
          <button :disabled="loading" type="submit">{{ loading ? '查询中…' : '搜索' }}</button>
          <button class="ghost" type="button" @click="attractionKeyword = ''; loadAttractions()">重置</button>
        </form>
        <div class="table-wrap"><table><thead><tr><th>景点</th><th>城市</th><th>类型</th><th>地址</th><th>录入时间</th></tr></thead><tbody>
          <tr v-for="item in attractions" :key="item.id"><td><strong>{{ item.name }}</strong><small>ID {{ item.id }}</small></td><td>{{ item.city }}</td><td><b class="category">{{ item.type || '未分类' }}</b></td><td class="address">{{ item.address || '暂无详细地址' }}</td><td>{{ formatDate(item.createTime) }}</td></tr>
        </tbody></table></div><p v-if="!attractions.length && !loading" class="empty">没有找到符合条件的景点。</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.admin-shell{display:grid;min-height:100vh;background:#f6f5f2;color:#282725;grid-template-columns:238px minmax(0,1fr)}.sidebar{position:sticky;top:0;display:flex;height:100vh;padding:28px 20px 22px;background:#242321;color:#fff;flex-direction:column}.brand{display:flex;padding:0 7px;align-items:center;gap:11px}.brand-mark{display:grid;width:39px;height:39px;border-radius:13px;background:#e8837d;color:#fff;font-family:var(--font-display);font-size:23px;place-items:center}.brand div{display:grid;gap:2px}.brand strong{font-size:17px}.brand small{color:#8e8982;font-size:8px;letter-spacing:.15em}.sidebar nav{display:grid;margin-top:45px;gap:7px}.sidebar nav button{display:flex;height:44px;padding:0 14px;border:0;border-radius:11px;background:transparent;color:#bcb8b2;cursor:pointer;align-items:center;gap:12px;text-align:left}.sidebar nav button span{width:20px;font-size:18px}.sidebar nav button:hover,.sidebar nav button.active{background:#373532;color:#fff}.sidebar nav button.active{box-shadow:inset 3px 0 #e8837d}.sidebar-bottom{display:grid;margin-top:auto;gap:8px}.sidebar-bottom a,.sign-out{padding:10px 12px;border:0;border-radius:9px;background:transparent;color:#aaa59e;font-size:12px;text-align:left;text-decoration:none}.sidebar-bottom a:hover,.sign-out:hover{background:#33312e;color:#fff}.sign-out{cursor:pointer}main{min-width:0;padding:38px clamp(28px,5vw,72px) 70px}.topbar{display:flex;align-items:center;justify-content:space-between}.topbar p{margin:0;color:#bd6d67;font-size:11px;font-weight:700;letter-spacing:.08em}.topbar h1{margin:6px 0 3px;font-size:34px;letter-spacing:-.03em}.topbar>div>span{color:#827d77;font-size:13px}.admin-account{display:flex;padding:8px 13px 8px 8px;border:1px solid #e0ddd7;border-radius:30px;background:#fff;align-items:center;gap:9px}.admin-account>span{display:grid;width:34px;height:34px;border-radius:50%;background:#f2cfca;color:#9d504d;place-items:center}.admin-account div{display:grid}.admin-account strong{font-size:12px}.admin-account small{color:#99938d;font-size:10px}.message{margin:22px 0 -6px;padding:11px 14px;border-radius:10px;background:#edf5ed;color:#416749;font-size:12px}.message.error{background:#fff0ed;color:#a64f52}.metric-grid{display:grid;margin-top:32px;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.metric-grid article{position:relative;display:grid;min-height:145px;padding:21px;overflow:hidden;border:1px solid #e5e1db;border-radius:17px;background:#fff;align-content:start}.metric-grid article::after{position:absolute;right:-22px;bottom:-31px;width:94px;height:94px;border-radius:50%;background:var(--bubble);content:''}.metric-grid span{color:#77716b;font-size:12px}.metric-grid strong{margin-top:8px;font-size:35px}.metric-grid small{margin-top:12px;color:#96908a;font-size:10px}.metric-grid .rose{--bubble:#fae3e0}.metric-grid .amber{--bubble:#f7ead3}.metric-grid .green{--bubble:#deebdf}.metric-grid .blue{--bubble:#e1e8ef}.overview-grid{display:grid;margin-top:16px;grid-template-columns:minmax(0,1.45fr) minmax(280px,.8fr);gap:16px}.overview-grid>article,.recent-card,.data-card{border:1px solid #e4e0da;border-radius:18px;background:#fff}.welcome-card{padding:28px;background:linear-gradient(125deg,#fff 55%,#fbebe7)!important}.welcome-card>span,.section-heading span{color:#bd6d67;font-size:11px}.welcome-card h2{margin:8px 0;font-size:23px}.welcome-card p{max-width:560px;margin:0;color:#77716b;font-size:13px;line-height:1.75}.welcome-card button,.section-heading button{margin-top:20px;padding:9px 13px;border:0;border-radius:18px;background:#2d2b29;color:#fff;cursor:pointer;font-size:11px}.health-card{padding:25px}.health-card>div{display:flex;align-items:center;gap:8px}.health-dot{width:9px;height:9px;border-radius:50%;background:#56a36b;box-shadow:0 0 0 5px #e7f3e9}.health-card p{color:#7f7973;font-size:12px}.health-card dl{display:grid;margin:18px 0 0;gap:9px}.health-card dl div{display:flex;padding-top:9px;border-top:1px solid #efede9;justify-content:space-between;font-size:11px}.health-card dt{color:#96908a}.health-card dd{margin:0}.recent-card{margin-top:16px;padding:24px}.section-heading{display:flex;align-items:flex-start;justify-content:space-between}.section-heading h2{margin:5px 0 0;font-size:19px}.section-heading button{margin:0;background:#f1eeea;color:#55514d}.mini-users{display:grid;margin-top:17px}.mini-users article{display:grid;padding:11px 0;border-top:1px solid #efede9;grid-template-columns:38px minmax(0,1fr) 75px 100px;align-items:center;gap:10px}.mini-avatar,.identity>span{display:grid;width:32px;height:32px;border-radius:50%;background:#f4dfda;color:#9b5955;font-size:12px;place-items:center}.mini-users article>div{display:grid}.mini-users strong{font-size:12px}.mini-users small{color:#98918c;font-size:10px}.mini-users b,.role{justify-self:start;padding:4px 7px;border-radius:9px;font-size:9px}.mini-users b.user,.role.user{background:#eef0f2;color:#69717a}.mini-users b.admin,.role.admin{background:#fbe0dc;color:#a9524d}.mini-users time{color:#96908a;font-size:10px}.data-card{margin-top:30px;padding:22px}.toolbar{display:flex;gap:8px}.toolbar input{width:min(420px,100%);padding:11px 13px;border:1px solid #ddd8d1;border-radius:10px;background:#faf9f7;outline:none}.toolbar button{padding:0 17px;border:0;border-radius:10px;background:#2e2c29;color:#fff;cursor:pointer}.toolbar button:disabled{opacity:.6}.toolbar .ghost{border:1px solid #ddd8d1;background:#fff;color:#68635e}.table-wrap{margin-top:20px;overflow:auto}table{width:100%;border-collapse:collapse;white-space:nowrap}th{padding:12px 10px;border-bottom:1px solid #ddd9d3;color:#8d8781;font-size:10px;font-weight:600;text-align:left}td{padding:15px 10px;border-bottom:1px solid #efede9;color:#5f5b56;font-size:11px}td>strong,td>small{display:block}.plain{color:#5f5b56;font-size:11px;font-weight:500}.identity{display:flex;align-items:center;gap:9px}.identity div{display:grid}.identity strong,td>strong{color:#2d2b29;font-size:12px}.identity small,td>small{margin-top:3px;color:#96908a;font-size:9px}.text-action{padding:6px 9px;border:1px solid #dfd9d2;border-radius:8px;background:#fff;color:#9c5754;cursor:pointer;font-size:10px}.text-action:disabled{cursor:not-allowed;opacity:.45}.status,.category{padding:4px 8px;border-radius:9px;background:#f5e6df;color:#96605b;font-size:9px}.address{max-width:300px;overflow:hidden;text-overflow:ellipsis}.empty{padding:45px 10px;color:#918b85;text-align:center}
@media(max-width:1050px){.metric-grid{grid-template-columns:repeat(2,1fr)}.overview-grid{grid-template-columns:1fr}}@media(max-width:760px){.admin-shell{grid-template-columns:1fr}.sidebar{position:static;height:auto;padding:16px}.brand{padding:0}.sidebar nav{display:flex;margin-top:18px;overflow-x:auto}.sidebar nav button{min-width:max-content}.sidebar-bottom{display:flex;margin-top:12px}.topbar{align-items:flex-start}.admin-account{display:none}main{padding:28px 16px 55px}.metric-grid{grid-template-columns:1fr 1fr}.mini-users article{grid-template-columns:38px minmax(0,1fr) 65px}.mini-users time{display:none}.toolbar{flex-wrap:wrap}.toolbar input{width:100%}}@media(max-width:480px){.metric-grid{grid-template-columns:1fr}.sidebar nav button{padding:0 10px}.sidebar nav button span{display:none}}
</style>
