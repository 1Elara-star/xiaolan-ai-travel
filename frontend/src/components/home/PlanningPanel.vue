<script setup lang="ts">
defineProps<{
  ideaSummary: string
}>()
</script>

<template>
  <aside class="planning-panel">
    <header class="panel-header">
      <div>
        <span>小兰陪你规划</span>
        <h2>下一段旅程</h2>
      </div>
      <button type="button" aria-label="更多操作">•••</button>
    </header>

    <div class="plan-preview">
      <p>你的旅行想法</p>
      <strong>{{ ideaSummary }}</strong>
      <span>这段内容目前只保存在页面中</span>
    </div>

    <div class="planning-steps">
      <h3>开始规划前</h3>
      <div class="step active">
        <b>1</b>
        <p><strong>选择目的城市</strong><span>发现适合你的旅行方向</span></p>
      </div>
      <div class="step">
        <b>2</b>
        <p><strong>收藏想去的景点</strong><span>了解故事和热门原因再决定</span></p>
      </div>
      <div class="step">
        <b>3</b>
        <p><strong>补充这次旅行需求</strong><span>日期、预算、同行人和节奏</span></p>
      </div>
      <div class="step">
        <b>4</b>
        <p><strong>确认后保存行程</strong><span>登录后再同步到账号</span></p>
      </div>
    </div>

    <div class="assistant-card">
      <div>
        <h3>不知道去哪也没关系</h3>
        <p>先写下喜欢的旅行节奏，之后接入外部能力时再由你亲手完成。</p>
      </div>
      <img src="/images/xiaolan-traveler.png" alt="小兰旅行助手" />
    </div>

    <button class="start-button" type="button"><span>开始探索城市</span><b>→</b></button>
  </aside>
</template>

<style scoped>
.planning-panel {
  min-width: 0;
  padding: 28px 25px;
  border-left: 1px solid var(--border-soft);
  background: rgba(255, 252, 248, 0.88);
}

.panel-header {
  display: flex;
  padding-bottom: 18px;
  border-bottom: 1px solid #efe4de;
  align-items: flex-start;
  justify-content: space-between;
}

.panel-header span {
  color: var(--coral);
  font-family: var(--font-display);
  font-size: 13px;
}

.panel-header h2 {
  margin: 5px 0 0;
  font-family: var(--font-display);
  font-size: 24px;
}

.panel-header button {
  width: 31px;
  height: 31px;
  border: 1px solid #ebdfd8;
  border-radius: 50%;
  background: #fffdfa;
  cursor: pointer;
}

.plan-preview {
  display: grid;
  margin-top: 20px;
  padding: 19px;
  border-radius: var(--radius-medium);
  background: #fbe9e6;
  gap: 7px;
}

.plan-preview p {
  margin: 0;
  color: #a1716c;
  font-size: 12px;
}

.plan-preview strong {
  font-family: var(--font-display);
  font-size: 17px;
  line-height: 1.45;
}

.plan-preview span {
  color: #8b7470;
  font-size: 12px;
}

.planning-steps {
  margin-top: 25px;
}

.planning-steps h3 {
  margin: 0 0 14px;
  font-size: 13px;
}

.step {
  position: relative;
  display: flex;
  min-height: 64px;
  padding: 8px 8px 8px 0;
  align-items: flex-start;
  gap: 11px;
}

.step:not(:last-child)::after {
  position: absolute;
  top: 35px;
  bottom: -2px;
  left: 13px;
  width: 1px;
  background: #e5d9d2;
  content: '';
}

.step b {
  position: relative;
  z-index: 1;
  display: grid;
  width: 27px;
  height: 27px;
  border: 1px solid #e7d9d2;
  border-radius: 50%;
  background: #fffaf6;
  color: #b4a39b;
  flex: 0 0 auto;
  font-size: 10px;
  place-items: center;
}

.step.active b {
  border-color: var(--coral-strong);
  background: var(--coral-strong);
  color: white;
}

.step p {
  display: grid;
  margin: 1px 0 0;
  gap: 4px;
}

.step strong {
  font-size: 11px;
}

.step p span {
  color: #978a84;
  font-size: 12px;
}

.assistant-card {
  position: relative;
  min-height: 185px;
  margin-top: 17px;
  padding: 20px 118px 18px 19px;
  overflow: hidden;
  border: 1px solid #eee1da;
  border-radius: var(--radius-medium);
  background:
    radial-gradient(circle at 95% 10%, rgba(247, 176, 162, 0.23), transparent 43%), #fffaf6;
}

.assistant-card h3 {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 16px;
}

.assistant-card p {
  margin: 0;
  color: #877872;
  font-size: 9px;
  line-height: 1.65;
}

.assistant-card img {
  position: absolute;
  right: -17px;
  bottom: -19px;
  width: 157px;
  mix-blend-mode: multiply;
}

.start-button {
  display: flex;
  width: 100%;
  height: 50px;
  margin-top: 20px;
  padding: 0 18px 0 21px;
  border: 0;
  border-radius: 25px;
  background: linear-gradient(135deg, #fa9292, #f2686d);
  box-shadow: 0 12px 24px rgba(238, 104, 109, 0.22);
  color: white;
  cursor: pointer;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
}

.start-button b {
  font-size: 19px;
}

@media (max-width: 1260px) {
  .planning-panel {
    display: none;
  }
}
</style>
