<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import * as memoryApi from '@/api/memories'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import type { MemoryType, UserMemory, UserMemoryRequest } from '@/types/memory'
import { getResponseMessage } from '@/utils/apiError'

const memories = ref<UserMemory[]>([])
const loading = ref(true)
const saving = ref(false)
const editingId = ref<number | null>(null)
const message = ref('')
const isError = ref(false)
const form = reactive<UserMemoryRequest>({
  memoryType: 'PREFERENCE',
  memoryContent: '',
  userConfirmed: true,
})

const memoryTypes: Array<{ value: MemoryType; label: string }> = [
  { value: 'PREFERENCE', label: '旅行偏好' },
  { value: 'DISLIKE', label: '不喜欢' },
  { value: 'EXPERIENCE', label: '旅行经验' },
  { value: 'REMINDER', label: '需要提醒' },
]

onMounted(loadMemories)

async function loadMemories() {
  loading.value = true
  try {
    memories.value = await memoryApi.listMemories()
  } catch (error) {
    showMessage(getResponseMessage(error) || '暂时无法读取旅行记忆。', true)
  } finally {
    loading.value = false
  }
}

function showMessage(value: string, error = false) {
  message.value = value
  isError.value = error
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { memoryType: 'PREFERENCE', memoryContent: '', userConfirmed: true })
}

function editMemory(memory: UserMemory) {
  editingId.value = memory.id
  Object.assign(form, {
    memoryType: memory.memoryType,
    memoryContent: memory.memoryContent,
    userConfirmed: memory.userConfirmed,
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function saveMemory() {
  if (!form.userConfirmed) {
    showMessage('请先确认这条内容可以作为长期旅行记忆。', true)
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await memoryApi.updateMemory(editingId.value, { ...form })
      showMessage('旅行记忆已更新。')
    } else {
      await memoryApi.createMemory({ ...form })
      showMessage('旅行记忆已保存。')
    }
    resetForm()
    memories.value = await memoryApi.listMemories()
  } catch (error) {
    showMessage(getResponseMessage(error) || '旅行记忆保存失败。', true)
  } finally {
    saving.value = false
  }
}

async function removeMemory(memory: UserMemory) {
  if (!window.confirm('确定删除这条旅行记忆吗？')) return
  try {
    await memoryApi.deleteMemory(memory.id)
    memories.value = memories.value.filter((item) => item.id !== memory.id)
    if (editingId.value === memory.id) resetForm()
    showMessage('旅行记忆已删除。')
  } catch (error) {
    showMessage(getResponseMessage(error) || '删除失败。', true)
  }
}

function typeLabel(type: MemoryType) {
  return memoryTypes.find((item) => item.value === type)?.label ?? type
}
</script>

<template>
  <div class="page-shell">
    <HomeSidebar active="memories" />
    <main>
      <header class="page-heading">
        <p>长期记忆</p>
        <h1>哪些事情，希望小兰记住</h1>
        <span>只有你确认的内容才应该进入长期记忆；聊天内容不会自动永久保存。</span>
      </header>

      <p v-if="message" class="message" :class="{ error: isError }">{{ message }}</p>

      <section class="memory-form">
        <div>
          <span>{{ editingId ? '修改记忆' : '新增记忆' }}</span>
          <h2>{{ editingId ? '调整这条内容' : '记下一件重要的事' }}</h2>
        </div>
        <form @submit.prevent="saveMemory">
          <label>
            <span>记忆类型</span>
            <select v-model="form.memoryType">
              <option v-for="type in memoryTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
          </label>
          <label class="content-field">
            <span>具体内容</span>
            <textarea
              v-model.trim="form.memoryContent"
              maxlength="500"
              required
              placeholder="例如：我不喜欢每天很早出发，希望行程留出休息时间。"
            />
          </label>
          <label class="confirmed-field">
            <input v-model="form.userConfirmed" type="checkbox" />
            <span>我确认将这条内容作为长期旅行记忆</span>
          </label>
          <div class="form-actions">
            <button v-if="editingId" type="button" class="secondary" @click="resetForm">取消编辑</button>
            <button type="submit" class="primary" :disabled="saving || !form.userConfirmed">
              {{ saving ? '保存中…' : editingId ? '保存修改' : '保存记忆' }}
            </button>
          </div>
        </form>
      </section>

      <section class="memory-list">
        <header><h2>我的旅行记忆</h2><span>{{ memories.length }} 条</span></header>
        <p v-if="loading" class="empty">正在读取…</p>
        <p v-else-if="memories.length === 0" class="empty">还没有保存旅行记忆。</p>
        <article v-for="memory in memories" v-else :key="memory.id">
          <div class="memory-meta">
            <span>{{ typeLabel(memory.memoryType) }}</span>
            <small>{{ memory.userConfirmed ? '已由你确认' : '尚未确认' }}</small>
          </div>
          <p>{{ memory.memoryContent }}</p>
          <div class="memory-actions">
            <button type="button" @click="editMemory(memory)">编辑</button>
            <button type="button" class="danger" @click="removeMemory(memory)">删除</button>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page-shell{display:grid;min-height:100vh;background:#faf6f1;grid-template-columns:178px minmax(0,1fr)}main{display:grid;width:min(900px,calc(100% - 42px));margin:0 auto;padding:46px 0 70px;align-content:start;gap:20px}.page-heading p{margin:0;color:var(--coral);font-size:12px}.page-heading h1{margin:8px 0;font-family:var(--font-display);font-size:clamp(32px,5vw,44px)}.page-heading span{color:var(--text-muted);font-size:13px}.message{margin:0;padding:11px 14px;border-radius:10px;background:#edf5ed;color:#416749;font-size:13px}.message.error{background:#fff0ed;color:#a64f52}.memory-form,.memory-list{padding:24px;border:1px solid #eadfd9;border-radius:19px;background:#fffaf6;box-shadow:var(--shadow-soft)}.memory-form>div>span{color:var(--coral);font-size:12px}h2{margin:5px 0 18px;font-size:20px}form{display:grid;grid-template-columns:190px 1fr;gap:15px}label{display:grid;gap:7px}label>span{color:#625651;font-size:12px}select,textarea{padding:11px 12px;border:1px solid #e7dad3;border-radius:10px;background:#fff;color:var(--text-main)}textarea{min-height:95px;resize:vertical}.content-field{grid-row:span 2}.confirmed-field{display:flex;align-items:center;gap:8px}.confirmed-field input{width:auto}.form-actions{display:flex;grid-column:1/-1;justify-content:flex-end;gap:8px}.form-actions button{padding:10px 17px;border-radius:19px;cursor:pointer}.secondary{border:1px solid #e6d9d2;background:white}.primary{border:0;background:var(--coral-strong);color:white}.memory-list>header{display:flex;align-items:center;justify-content:space-between}.memory-list>header span{color:var(--text-muted);font-size:12px}.memory-list article{position:relative;padding:17px 95px 17px 0;border-top:1px solid #eee2dc}.memory-meta{display:flex;align-items:center;gap:10px}.memory-meta span{padding:4px 8px;border-radius:10px;background:#f9e5df;color:#a95e5e;font-size:11px}.memory-meta small{color:var(--text-muted)}.memory-list article p{margin:10px 0 0;line-height:1.7}.memory-actions{position:absolute;top:16px;right:0;display:flex;gap:5px}.memory-actions button{border:0;background:transparent;color:#796b65;cursor:pointer}.memory-actions .danger{color:#ad575b}.empty{padding:28px 0;color:var(--text-muted);text-align:center}
@media(max-width:900px){.page-shell{display:block}}@media(max-width:620px){form{grid-template-columns:1fr}.content-field{grid-row:auto}.form-actions{grid-column:auto}.memory-list article{padding-right:0;padding-bottom:50px}.memory-actions{top:auto;right:auto;bottom:15px;left:0}}
</style>
