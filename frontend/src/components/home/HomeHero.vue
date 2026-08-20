<script setup lang="ts">
import { travelTags } from '@/data/home'

const travelIdea = defineModel<string>('travelIdea', { required: true })
const activeTag = defineModel<string>('activeTag', { required: true })

defineProps<{
  saveMessage?: string
}>()

const emit = defineEmits<{
  save: []
}>()

const tagIdeas: Record<string, string> = {
  海边: '想去海边住几天，看看日落，行程不要太赶',
  小众城市: '想找一座游客不太多的小城市，慢慢逛当地街区',
  历史文化: '想安排一次以历史建筑和当地故事为主的旅行',
  美食之旅: '想围绕当地特色美食规划旅行，也想留出散步时间',
}

function selectTag(tag: string) {
  activeTag.value = tag
  travelIdea.value = tagIdeas[tag] ?? tag
}
</script>

<template>
  <section class="hero-section">
    <div class="hero-copy">
      <h1>和小兰说说，你想怎么旅行</h1>
      <span class="coral-stroke" aria-hidden="true"></span>
      <p>一句话也可以：想去哪里、和谁出发，<br />还有你不喜欢太赶的小习惯。</p>

      <div class="idea-box">
        <label class="idea-input">
          <span aria-hidden="true">⌕</span>
          <input
            v-model="travelIdea"
            type="text"
            placeholder="例如：国庆去厦门，3 天，想看海但不想走太累"
          />
           <button type="button" aria-label="保存旅行想法" @click="emit('save')">保存</button>
        </label>
        <div class="hot-tags">
          <span>热门想法：</span>
          <button
            v-for="tag in travelTags"
            :key="tag"
            type="button"
            :class="{ active: activeTag === tag }"
            @click="selectTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
        <p v-if="saveMessage" class="save-message" role="status">{{ saveMessage }}</p>
      </div>
    </div>

    <figure class="hero-postcard">
      <span class="tape" aria-hidden="true"></span>
      <img src="/images/hero-journey.jpg" alt="阳光下的旅行街景" />
      <figcaption>下一段旅程</figcaption>
      <small>旅行灵感</small>
      <span class="postmark" aria-hidden="true">BON VOYAGE</span>
    </figure>
  </section>
</template>

<style scoped>
.hero-section {
  position: relative;
  display: grid;
  min-height: 380px;
  padding: 26px 12px 12px 10px;
  align-items: center;
  grid-template-columns: minmax(420px, 1fr) 330px;
  gap: 30px;
}

.hero-section::before {
  position: absolute;
  inset: 6px -15px 0 -8px;
  z-index: -1;
  border-radius: 28px;
  background:
    radial-gradient(circle at 24% 30%, rgba(245, 172, 154, 0.13), transparent 38%),
    radial-gradient(circle at 75% 66%, rgba(223, 195, 165, 0.15), transparent 40%);
  content: '';
}

.hero-copy {
  position: relative;
  z-index: 1;
  padding-left: 32px;
}

h1 {
  max-width: 550px;
  margin: 0;
  color: var(--text-main);
  font-family: var(--font-display);
  font-size: clamp(42px, 4.3vw, 64px);
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1.15;
}

.coral-stroke {
  display: block;
  width: 245px;
  height: 24px;
  margin: 0 0 4px 76px;
  border-top: 4px solid var(--coral);
  border-radius: 50%;
  transform: rotate(-5deg);
}

.hero-copy > p {
  margin: 5px 0 26px 55px;
  color: #675b56;
  font-size: 14px;
  line-height: 1.8;
}

.idea-box {
  width: min(500px, 100%);
  padding: 13px 15px 11px;
  border: 1px solid #eaded7;
  border-radius: var(--radius-large);
  background: rgba(255, 252, 248, 0.92);
  box-shadow: var(--shadow-card);
}

.save-message {
  margin: 9px 5px 0;
  color: #7b6e68;
  font-size: 12px;
}

.idea-input {
  display: flex;
  height: 42px;
  padding: 0 7px 0 10px;
  border: 1px solid #eee4de;
  border-radius: 22px;
  background: #fffdfb;
  align-items: center;
  gap: 8px;
}

.idea-input input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  flex: 1;
  font-size: 12px;
}

.idea-input button {
  min-width: 48px;
  height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 50%;
  background: var(--coral-strong);
  box-shadow: 0 5px 12px rgba(239, 104, 109, 0.25);
  color: white;
  cursor: pointer;
  font-size: 12px;
}

.hot-tags {
  display: flex;
  padding: 10px 3px 0;
  align-items: center;
  gap: 7px;
  overflow-x: auto;
  white-space: nowrap;
}

.hot-tags span {
  color: #9b8d87;
  font-size: 12px;
}

.hot-tags button {
  padding: 5px 9px;
  border: 0;
  border-radius: 12px;
  background: #f6f0eb;
  color: #766963;
  cursor: pointer;
  font-size: 12px;
}

.hot-tags button.active {
  background: var(--coral-light);
  color: #bd5e61;
}

.hero-postcard {
  position: relative;
  width: 280px;
  margin: 0;
  padding: 13px 13px 24px;
  border: 1px solid #e7dcd4;
  border-radius: 3px;
  background: #fffaf5;
  box-shadow: 0 20px 40px rgba(75, 54, 44, 0.16);
  transform: rotate(2deg);
}

.hero-postcard img {
  width: 100%;
  height: 248px;
  object-fit: cover;
}

.hero-postcard figcaption {
  margin: 9px 0 0 14px;
  color: #72635c;
  font-family: var(--font-display);
  font-size: 16px;
}

.hero-postcard small {
  display: block;
  margin: 3px 13px 0 0;
  color: #9a8981;
  font-family: monospace;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-align: right;
}

.tape {
  position: absolute;
  top: -12px;
  right: 35px;
  z-index: 2;
  width: 78px;
  height: 22px;
  background: rgba(218, 180, 143, 0.44);
  transform: rotate(-4deg);
}

.postmark {
  position: absolute;
  top: 126px;
  left: -48px;
  display: grid;
  width: 77px;
  height: 77px;
  border: 2px double rgba(112, 93, 83, 0.38);
  border-radius: 50%;
  color: rgba(112, 93, 83, 0.5);
  font-family: monospace;
  font-size: 8px;
  transform: rotate(-16deg);
  place-items: center;
}

@media (max-width: 900px) {
  .hero-section {
    grid-template-columns: 1fr 250px;
  }

  .hero-copy {
    padding-left: 0;
  }

  h1 {
    font-size: 43px;
  }

  .hero-postcard {
    width: 225px;
  }

  .hero-postcard img {
    height: 190px;
  }
}

@media (max-width: 640px) {
  .hero-section {
    display: flex;
    min-height: auto;
    padding-top: 8px;
    padding-bottom: 48px;
    flex-direction: column;
  }

  .hero-copy {
    width: 100%;
  }

  h1 {
    font-size: 38px;
  }

  .hero-copy > p {
    margin-left: 0;
  }

  .coral-stroke {
    margin-left: 35px;
  }

  .hero-postcard {
    display: none;
  }
}
</style>
