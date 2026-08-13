<template>
  <section class="discovery-strip">
    <div>
      <span>灵感发现</span>
      <h2>每一座城市，都有独特的温度。</h2>
    </div>
    <ol>
      <li><b>01</b><span>选择想去的城市</span></li>
      <li><b>02</b><span>读故事，收藏心动景点</span></li>
      <li><b>03</b><span>带着收藏开始规划</span></li>
    </ol>
  </section>
</template>

<style scoped>
.discovery-strip {
  display: flex;
  margin-top: 24px;
  padding: 21px 24px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-medium);
  background: rgba(255, 252, 248, 0.82);
  align-items: center;
  justify-content: space-between;
}

.discovery-strip > div > span {
  color: var(--coral);
  font-family: var(--font-display);
  font-size: 17px;
}

h2 {
  margin: 5px 0 0;
  color: #776a64;
  font-size: 13px;
  font-weight: 500;
}

ol {
  display: flex;
  margin: 0;
  padding: 0;
  gap: 20px;
  list-style: none;
}

li {
  display: flex;
  align-items: center;
  gap: 7px;
}

b {
  color: var(--coral);
  font-family: Georgia, serif;
  font-size: 16px;
}

li span {
  color: #695e59;
  font-size: 12px;
}

@media (max-width: 640px) {
  .discovery-strip {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  ol {
    display: grid;
    gap: 12px;
  }
}
</style>
