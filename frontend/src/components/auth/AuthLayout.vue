<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="auth-page">
    <section class="travel-side">
      <RouterLink class="brand" to="/">小兰 <span>AI Travel</span></RouterLink>
      <div class="travel-copy">
        <p>一路上的想法，都值得被好好记住。</p>
        <h1>回来继续你的旅行</h1>
        <span
          >登录后可以保存行程和旅行偏好。只想逛逛也没关系，首页和城市探索始终可以直接访问。</span
        >
      </div>
      <img src="/images/xiaolan-traveler.png" alt="小兰旅行助手" />
    </section>

    <section class="form-side">
      <RouterLink class="back-home" to="/">← 返回首页</RouterLink>
      <div class="form-card"><slot /></div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  padding: 28px;
  background: #f7f0eb;
  grid-template-columns: minmax(360px, 0.9fr) minmax(420px, 1.1fr);
}

.travel-side {
  position: relative;
  display: flex;
  min-height: calc(100vh - 56px);
  overflow: hidden;
  padding: 42px;
  border-radius: 26px;
  background:
    linear-gradient(rgba(255, 249, 244, 0.88), rgba(255, 244, 237, 0.9)),
    url('/images/hero-journey.jpg') center / cover;
  flex-direction: column;
}

.brand {
  color: var(--text-main);
  font-family: var(--font-display);
  font-size: 31px;
  font-weight: 800;
  text-decoration: none;
}

.brand span {
  margin-left: 4px;
  color: var(--coral);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
}

.travel-copy {
  position: relative;
  z-index: 1;
  max-width: 480px;
  margin-top: 16vh;
}

.travel-copy p {
  color: var(--coral);
  font-size: 13px;
  font-weight: 700;
}

.travel-copy h1 {
  margin: 12px 0;
  font-family: var(--font-display);
  font-size: clamp(38px, 5vw, 58px);
  line-height: 1.2;
}

.travel-copy span {
  display: block;
  max-width: 420px;
  color: #756862;
  font-size: 14px;
  line-height: 1.8;
}

.travel-side img {
  position: absolute;
  right: 12px;
  bottom: -22px;
  width: min(250px, 40%);
  mix-blend-mode: multiply;
}

.form-side {
  display: grid;
  min-width: 0;
  padding: 22px clamp(30px, 7vw, 100px);
  align-content: center;
}

.back-home {
  position: absolute;
  top: 42px;
  color: #776b65;
  font-size: 13px;
  text-decoration: none;
}

.form-card {
  width: 100%;
  max-width: 430px;
  min-width: 0;
  margin: 0 auto;
}

@media (max-width: 850px) {
  .auth-page {
    display: block;
    padding: 0;
  }

  .travel-side {
    min-height: 230px;
    padding: 28px 24px;
    border-radius: 0 0 26px 26px;
  }

  .travel-copy {
    margin-top: 30px;
  }

  .travel-copy p,
  .travel-copy span {
    display: none;
  }

  .travel-copy h1 {
    max-width: 260px;
    font-size: 36px;
  }

  .travel-side img {
    width: 160px;
  }

  .form-side {
    width: 100%;
    padding: 50px 24px;
  }

  .back-home {
    position: static;
    margin-bottom: 30px;
  }
}
</style>
