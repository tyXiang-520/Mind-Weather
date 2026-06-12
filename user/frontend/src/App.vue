<template>
  <div class="app-root">
    <HomePage v-if="currentTab === 'home'" />
    <PostPage v-else-if="currentTab === 'post'" />
    <MyPage v-else-if="currentTab === 'my'" />

    <nav class="tab-bar">
      <button
        :class="['tab-item', { active: currentTab === 'home' }]"
        @click="currentTab = 'home'"
      >
        <span class="tab-icon">🏠</span>
        <span>首页</span>
      </button>

      <button class="tab-item tab-post" @click="currentTab = 'post'">
        <div class="post-btn">
          <span class="post-icon">+</span>
        </div>
        <span>发布心情</span>
      </button>

      <button
        :class="['tab-item', { active: currentTab === 'my' }]"
        @click="currentTab = 'my'"
      >
        <span class="tab-icon">🌤️</span>
        <span>我的天气</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import HomePage from './views/HomePage.vue'
import PostPage from './views/PostPage.vue'
import MyPage from './views/MyPage.vue'

const currentTab = ref('home')
</script>

<style scoped>
.app-root {
  width: 100%;
  height: 100%;
  position: relative;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom, 6px);
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: rgba(10, 10, 28, 0.82);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 24px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 10px;
  cursor: pointer;
  border: none;
  background: none;
  transition: color 0.2s;
}

.tab-item.active {
  color: #7ec8e3;
}

.tab-icon {
  font-size: 22px;
  line-height: 1;
}

/* Middle post button */
.tab-post {
  gap: 3px;
  padding: 2px 20px;
}

.post-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7ec8e3, #5a9ec0);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(94, 158, 192, 0.45), 0 0 12px rgba(126, 200, 227, 0.35);
  margin-top: -20px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  position: relative;
}

.post-btn::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(126, 200, 227, 0.25), transparent 70%);
  z-index: -1;
  pointer-events: none;
}

.post-icon {
  font-size: 28px;
  font-weight: 300;
  color: #fff;
  line-height: 1;
}
</style>
