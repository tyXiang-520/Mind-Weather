<template>
  <div class="app-root">
    <!-- 页面过渡区域 -->
    <div class="view-area" :class="{ 'view-enter': viewAnimating }">
      <!-- 首页（v-if 销毁 3D 场景以释放 GPU） -->
      <HomePage v-if="currentView === 'home' && !isDebug" @building-click="openBuilding" @building-detail="openBuilding" />

      <!-- 投稿页（v-show 保留表单输入状态） -->
      <PostPage v-show="currentView === 'post'" />

      <!-- 我的天气（v-if 释放资源） -->
      <ProfilePage v-if="currentView === 'my'" @navigate="navigate" />

      <!-- Sub pages (v-if 按需创建) -->
      <BuildingPage
        v-if="currentView === 'building'"
        :buildingName="buildingName"
        @back="currentView = 'home'"
      />
      <RegisterPage v-if="currentView === 'register'" @navigate="navigate" />

      <!-- Debug page（独立路由，无 tab 栏）-->
      <DebugPage v-if="isDebug" />
    </div>

    <!-- Tab bar（debug 模式隐藏）-->
    <nav v-if="!isDebug" class="tab-bar">
      <button
        :class="['tab-item', { active: currentView === 'home' }]"
        @click="switchTab('home')"
      >
        <span class="tab-icon">🏠</span>
        <span>首页</span>
      </button>

      <button class="tab-item tab-post" @click="switchTab('post')">
        <div class="post-btn">
          <span class="post-icon">+</span>
        </div>
        <span>发布心情</span>
      </button>

      <button
        :class="['tab-item', { active: currentView === 'my' }]"
        @click="switchTab('my')"
      >
        <span class="tab-icon">🌤️</span>
        <span>我的天气</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { createLogger } from './utils/debug.js'
import HomePage from './views/HomePage.vue'
import PostPage from './views/PostPage.vue'
import ProfilePage from './views/Profile.vue'
import BuildingPage from './views/Building.vue'
import RegisterPage from './views/Register.vue'
import DebugPage from './views/DebugPage.vue'

const log = createLogger('App')

const currentView = ref('home')
const buildingName = ref('')
const isDebug = ref(false)
const viewAnimating = ref(false)

// 页面切换触发呼吸动画
watch(currentView, () => {
  viewAnimating.value = true
  setTimeout(() => { viewAnimating.value = false }, 400)
})

// 检测 URL 是否为 /debug 路径
function checkDebugRoute() {
  log.log('checkDebugRoute', { path: window.location.pathname, hash: window.location.hash })
  isDebug.value = window.location.pathname === '/debug' || window.location.hash === '#/debug'
}

function switchTab(tab) {
  log.log('switchTab', { from: currentView.value, to: tab })
  currentView.value = tab
  buildingName.value = ''
}

function openBuilding(name) {
  log.log('openBuilding', { name, currentView: currentView.value })
  buildingName.value = name
  currentView.value = 'building'
}

function navigate(view, param) {
  log.log('navigate', { view, param, from: currentView.value })
  if (view === 'building') {
    buildingName.value = param
    currentView.value = 'building'
  } else if (view === 'home' || view === 'my' || view === 'post') {
    currentView.value = view
  } else {
    currentView.value = view
  }
}

onMounted(() => {
  checkDebugRoute()
  window.addEventListener('hashchange', checkDebugRoute)
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', checkDebugRoute)
})
</script>

<style scoped>
.app-root {
  width: 100%;
  height: 100%;
  position: relative;
}

.view-area {
  width: 100%;
  height: 100%;
  transition: opacity 0.3s ease;
}

.view-area.view-enter {
  animation: viewFadeIn 0.4s ease;
}

@keyframes viewFadeIn {
  0% { opacity: 0.6; transform: scale(0.98); }
  100% { opacity: 1; transform: scale(1); }
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
