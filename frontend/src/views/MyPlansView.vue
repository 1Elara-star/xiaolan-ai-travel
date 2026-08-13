<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { deletePlan, listMyPlans } from '@/api/travel'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import type { TravelPlan } from '@/types/travel'
import { getResponseMessage } from '@/utils/apiError'

const plans = ref<TravelPlan[]>([])
const loading = ref(true)
const message = ref('')
async function load(){loading.value=true;try{plans.value=await listMyPlans()}catch(e){message.value=getResponseMessage(e)||'暂时无法读取行程。'}finally{loading.value=false}}
async function remove(plan:TravelPlan){if(!window.confirm(`确定删除“${plan.title}”吗？`))return;try{await deletePlan(plan.id);plans.value=plans.value.filter(x=>x.id!==plan.id)}catch(e){message.value=getResponseMessage(e)||'删除失败。'}}
onMounted(load)
</script>
<template><div class="page-shell"><HomeSidebar active="plans"/><main><div class="heading"><div><p>我的旅行</p><h1>已经记下的行程</h1></div><RouterLink to="/plan/new">新建行程</RouterLink></div><p v-if="message" class="message">{{message}}</p><p v-if="loading" class="empty">正在读取…</p><section v-else-if="plans.length" class="grid"><article v-for="plan in plans" :key="plan.id"><div class="status">{{plan.tripStatus==='PLANNING'?'准备中':plan.tripStatus}}</div><h2>{{plan.title}}</h2><p>{{plan.departureCity}} → {{plan.destination}}</p><strong>{{plan.startDate}} — {{plan.endDate}}</strong><span>{{plan.travelDays}} 天 · {{plan.peopleCount}} 人<span v-if="plan.budget"> · ¥{{plan.budget}}</span></span><small>{{plan.tripPreferences||'还没有补充旅行偏好'}}</small><button type="button" @click="remove(plan)">删除</button></article></section><div v-else class="empty"><p>还没有行程。</p><RouterLink to="/plan/new">写下第一趟旅行</RouterLink></div></main></div></template>
<style scoped>.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{width:min(1040px,calc(100% - 50px));margin:0 auto;padding:48px 0}.heading{display:flex;align-items:end;justify-content:space-between}.heading p{margin:0;color:var(--coral);font-size:12px}.heading h1{margin:8px 0 0;font-family:var(--font-display);font-size:42px}.heading a,.empty a{padding:11px 18px;border-radius:22px;background:var(--coral-strong);color:white;text-decoration:none;font-size:13px}.grid{display:grid;margin-top:30px;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}article{position:relative;display:grid;padding:24px;border:1px solid #eadfd9;border-radius:18px;background:#fffaf6;box-shadow:var(--shadow-soft);gap:9px}article h2,article p{margin:0}article span,article small{color:var(--text-muted)}article button{justify-self:start;margin-top:10px;border:0;background:transparent;color:#ad6263;cursor:pointer}.status{position:absolute;top:20px;right:20px;padding:4px 8px;border-radius:10px;background:var(--coral-light);font-size:11px}.empty{margin-top:45px;color:var(--text-muted)}.empty a{display:inline-block;margin-top:8px}.message{color:#b75458}@media(max-width:900px){.page-shell{display:block}}@media(max-width:650px){.grid{grid-template-columns:1fr}.heading h1{font-size:34px}}</style>
