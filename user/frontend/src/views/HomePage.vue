<template>
  <div class="home-page">
    <!-- 3D 校园场景（GLB 模型） -->
    <NJU3D
      ref="nju3dRef"
      class="scene-layer"
      :weather="displayWeather"
      @building-click="onBuildingClick"
      @ready="onSceneReady"
    />

    <!-- Top title bar -->
    <div class="top-bar">
      <div class="logo">
        <div class="logo-icon">☁️</div>
        <div class="logo-text">
          <div class="logo-main">MindWeather</div>
          <div class="logo-sub">你的心情，映照天气</div>
        </div>
      </div>
    </div>

    <!-- 建筑投稿面板（点击建筑后弹出） -->
    <div class="building-panel" :class="{ visible: selectedBuilding }">
      <div class="building-panel-inner">
        <div class="bp-header">
          <div class="bp-building-info">
            <div class="bp-building-name">{{ selectedBuilding?.displayName }}</div>
            <div class="bp-zone-tag" v-if="selectedBuilding?.zone">
              {{ selectedBuilding.zone.id }}区 {{ selectedBuilding.zone.name }}
            </div>
          </div>
          <button class="bp-close" @click="selectedBuilding = null">✕</button>
        </div>
        <div class="bp-posts">
          <div class="bp-posts-title">最近心情</div>
          <div v-if="buildingPosts.length === 0" class="bp-empty">
            这里还没有人分享心情，成为第一个吧 🌱
          </div>
          <div v-for="post in buildingPosts" :key="post.id" class="bp-post-item">
            <div class="bp-post-emoji">{{ post.weatherIcon }}</div>
            <div class="bp-post-body">
              <div class="bp-post-text">{{ post.content }}</div>
              <div class="bp-post-meta">{{ post.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom glass drawer -->
    <div class="drawer" :class="{ collapsed: drawerCollapsed }">
      <div class="drawer-inner">
        <!-- Handle -->
        <div class="drawer-handle" @click="drawerCollapsed = !drawerCollapsed"></div>

        <!-- Campus weather summary -->
        <div class="summary-section">
          <div class="summary-header">
            <span class="summary-emoji">{{ mainWeatherIcon }}</span>
            <div class="summary-text">
              <div class="summary-label">全校心情总览</div>
              <div class="summary-name">{{ campusWeatherName }}</div>
            </div>
          </div>

          <!-- Donut chart -->
          <div class="chart-row">
            <svg class="donut-chart" viewBox="0 0 100 100">
              <circle
                v-for="(seg, i) in donutSegments"
                :key="i"
                cx="50" cy="50" r="40"
                fill="none"
                :stroke="seg.color"
                stroke-width="12"
                :stroke-dasharray="seg.dash"
                :stroke-dashoffset="seg.offset"
                stroke-linecap="round"
              />
            </svg>
            <div class="chart-legend">
              <div v-for="item in emotionStats" :key="item.name" class="legend-item">
                <span class="legend-dot" :style="{ background: item.color }"></span>
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-pct">{{ item.percent }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Hot comments -->
        <div class="comments-section">
          <div class="section-title">
            大家正在说
            <span class="section-count">({{ totalComments }})</span>
          </div>
          <div class="comment-list">
            <div v-for="comment in hotComments" :key="comment.id" class="comment-item">
              <img class="comment-avatar" :src="comment.avatar" alt="avatar" />
              <div class="comment-body">
                <div class="comment-text">{{ comment.text }}</div>
                <div class="comment-time">{{ comment.time }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Hot tags -->
        <div class="tags-section">
          <div class="section-title">热门标签</div>
          <div class="tags-row">
            <span v-for="tag in hotTags" :key="tag.name" class="tag-capsule">
              #{{ tag.name }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import NJU3D from '../components/NJU3D.vue'
import { api } from '../api/index.js'

const nju3dRef = ref(null)
const drawerCollapsed = ref(true)
const campusWeatherName = ref('多云')
const totalComments = ref(0)
const displayWeather = ref('cloudy')
const sceneReady = ref(false)

// 选中建筑
const selectedBuilding = ref(null)
const buildingPosts = ref([])

// Mock 投稿数据池
const MOCK_POSTS = [
  { weatherIcon: '☀️', content: '今天北大楼前面阳光真好，拍照超美！', time: '3 分钟前' },
  { weatherIcon: '⛅', content: '图书馆自习到崩溃，谁来救救我...', time: '12 分钟前' },
  { weatherIcon: '🌧️', content: 'ddl快到了，心情像下雨一样沉重', time: '28 分钟前' },
  { weatherIcon: '☀️', content: '在大礼堂听了一场很棒的讲座', time: '1 小时前' },
  { weatherIcon: '⛅', content: '梧桐大道太美了，秋天的校园就是最好的', time: '1 小时前' },
  { weatherIcon: '🌩️', content: '考试周压力爆炸，头发一把把掉', time: '2 小时前' },
  { weatherIcon: '☀️', content: '食堂今天的麻辣香锅格外好吃！', time: '3 小时前' },
  { weatherIcon: '🌧️', content: '操场上跑了五公里，浑身湿透但很爽', time: '4 小时前' },
]

// ═══════════ 主天气图标 ═══════════
const mainWeatherIcon = computed(() => {
  const icons = {
    sunny: '☀️', cloudy: '⛅', overcast: '☁️',
    rainy: '🌧️', heavy_rain: '⛈️', thunderstorm: '🌩️'
  }
  return icons[displayWeather.value] || '⛅'
})

// ═══════════ 情绪统计（Mock，后续从 API 获取） ═══════════
const emotionStats = ref([
  { name: '愉悦', percent: 32, color: '#f0a860' },
  { name: '平静', percent: 28, color: '#5ac8a0' },
  { name: '焦虑', percent: 23, color: '#7ec8e3' },
  { name: '低落', percent: 12, color: '#a080d0' },
  { name: '其他', percent: 5, color: '#888888' },
])

const donutSegments = computed(() => {
  const r = 40
  const circumference = 2 * Math.PI * r
  let offset = 0
  return emotionStats.value.map(item => {
    const dashLen = (item.percent / 100) * circumference
    const seg = { color: item.color, dash: `${dashLen} ${circumference - dashLen}`, offset: -offset }
    offset += dashLen
    return seg
  })
})

const hotComments = ref([
  { id: 1, text: '图书馆自习好累...', time: '1 分钟前', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=1' },
  { id: 2, text: '今天阳光真好！', time: '3 分钟前', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=2' },
  { id: 3, text: 'ddl快来了T_T', time: '5 分钟前', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=3' },
])

const hotTags = ref([
  { name: '考试周' }, { name: 'DDL' }, { name: '小组作业' }, { name: '保研' }, { name: '实习' },
])

// ═══════════ 事件 ═══════════
function onBuildingClick(data) {
  console.log('点击建筑:', data.displayName || data.name, '分区:', data.zone)

  // 避免重复点击同一建筑
  if (selectedBuilding.value?.name === data.name) {
    selectedBuilding.value = null
    buildingPosts.value = []
    return
  }

  selectedBuilding.value = data

  // 生成 mock 投稿（随机取 2-4 条）
  const count = 2 + Math.floor(Math.random() * 3)
  const shuffled = [...MOCK_POSTS].sort(() => Math.random() - 0.5)
  buildingPosts.value = shuffled.slice(0, count).map((p, i) => ({
    ...p,
    id: Date.now() + i
  }))

  // 收起抽屉
  drawerCollapsed.value = true
}

function onSceneReady() {
  sceneReady.value = true
}

// ═══════════ API ═══════════
onMounted(async () => {
  try {
    const res = await api.getMapOverview()
    if (res.code === 0 && res.data) {
      totalComments.value = res.data.totalPostsToday || 0
      const cw = res.data.campusMainWeather
      if (cw) {
        campusWeatherName.value = cw.weatherName || '多云'
        displayWeather.value = mapWeatherCode(cw.weatherCode)
      }
      if (res.data.hotTags) {
        hotTags.value = res.data.hotTags.slice(0, 8).map(t => ({ name: t.name }))
      }
    }
  } catch (e) {
    console.log('API 未连接，使用默认数据')
  }
})

function mapWeatherCode(code) {
  const map = {
    sunny: 'sunny', cloudy: 'cloudy', overcast: 'overcast',
    rainy: 'rainy', heavy_rain: 'heavy_rain', thunderstorm: 'thunderstorm'
  }
  return map[code] || 'cloudy'
}
</script>

<style scoped>
.home-page {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #1a2a40;
}

.scene-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* ── Top bar ── */
.top-bar {
  position: fixed;
  top: 16px;
  left: 0;
  right: 0;
  z-index: 20;
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(10, 10, 30, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 8px 18px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon { font-size: 22px; }

.logo-main {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.logo-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 2px;
}

/* ── Bottom drawer ── */
.drawer {
  position: fixed;
  bottom: 72px;
  left: 10px;
  right: 10px;
  z-index: 20;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.drawer.collapsed {
  transform: translateY(calc(100% - 42px));
}

.drawer-inner {
  background: rgba(14, 14, 36, 0.72);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  padding: 12px 18px 18px;
  max-height: 58vh;
  overflow-y: auto;
  scrollbar-width: none;
}

.drawer-inner::-webkit-scrollbar { display: none; }

.drawer-handle {
  width: 36px;
  height: 4px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 2px;
  margin: 0 auto 12px;
  cursor: pointer;
}

/* ── Summary ── */
.summary-section { margin-bottom: 14px; }

.summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-emoji { font-size: 36px; line-height: 1; }

.summary-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
}

.summary-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

/* ── Donut chart ── */
.chart-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.donut-chart {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  transform: rotate(-90deg);
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name { color: rgba(255, 255, 255, 0.7); }

.legend-pct {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

/* ── Comments ── */
.comments-section {
  margin-bottom: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 10px;
}

.section-count {
  color: rgba(255, 255, 255, 0.35);
  font-weight: 400;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.comment-body { flex: 1; min-width: 0; }

.comment-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.comment-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 2px;
}

/* ── Tags ── */
.tags-section {
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-capsule {
  padding: 6px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}

/* ── 建筑投稿面板 ── */
.building-panel {
  position: fixed;
  bottom: 76px;
  left: 10px;
  right: 10px;
  z-index: 25;
  transform: translateY(120%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}

.building-panel.visible {
  transform: translateY(0);
  pointer-events: auto;
}

.building-panel-inner {
  background: rgba(14, 14, 40, 0.78);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 14px 18px 18px;
  max-height: 45vh;
  overflow-y: auto;
}

.bp-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.bp-building-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.bp-zone-tag {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(126, 200, 227, 0.15);
  border: 1px solid rgba(126, 200, 227, 0.2);
  font-size: 11px;
  color: rgba(126, 200, 227, 0.9);
}

.bp-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bp-posts-title {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.bp-empty {
  text-align: center;
  padding: 24px 0;
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
}

.bp-post-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.bp-post-item:last-child { border-bottom: none; }

.bp-post-emoji {
  font-size: 22px;
  flex-shrink: 0;
}

.bp-post-body {
  flex: 1;
  min-width: 0;
}

.bp-post-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.4;
}

.bp-post-meta {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  margin-top: 4px;
}
</style>
