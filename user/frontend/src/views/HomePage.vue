<template>
  <div class="home-page">
    <!-- 3D 校园场景（生产模式，无调试面板） -->
    <NJU3D
      ref="nju3dRef"
      class="scene-layer"
      :weather="displayWeather"
      :debug="false"
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
      <div class="top-actions">
        <button class="overview-btn" @click="showOverview = true">🗺️ 天气总览</button>
      </div>
    </div>

    <!-- 天气总览面板 -->
    <WeatherOverviewPanel
      :show="showOverview"
      :zones="overviewZones"
      @close="showOverview = false"
    />

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
        <!-- 区域统计 -->
        <div class="bp-stats" v-if="selectedZoneData">
          <div class="bp-stat-row">
            <span class="bp-stat-label">天气</span>
            <span class="bp-stat-value">{{ getWeatherIcon(selectedZoneData.weatherCode) }} {{ selectedZoneData.weatherName || '多云' }}</span>
          </div>
          <div class="bp-stat-row">
            <span class="bp-stat-label">投稿数</span>
            <span class="bp-stat-value">{{ selectedZoneData.postCount || 0 }} 篇</span>
          </div>
          <div class="bp-stat-row">
            <span class="bp-stat-label">主导情绪</span>
            <span class="bp-stat-value">{{ selectedZoneData.dominantEmotion || '平静' }}</span>
          </div>
        </div>
        <!-- 该建筑最近投稿 -->
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
          <button
            v-if="selectedBuilding"
            class="bp-view-more"
            @click="viewBuildingDetail"
          >查看全部动态 →</button>
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

        <!-- 最新投稿 -->
        <div class="comments-section">
          <div class="section-title">
            大家正在说
            <span class="section-count">({{ hotComments.length }})</span>
          </div>
          <div class="comment-list">
            <div v-for="post in hotComments" :key="post.id" class="comment-item">
              <span class="comment-avatar-emoji">{{ post.authorAvatar || '👤' }}</span>
              <div class="comment-body">
                <div class="comment-text">{{ post.content }}</div>
                <div class="comment-time">{{ post.authorName }} · {{ post.buildingName }} · {{ post.createdAt }}</div>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { createLogger } from '../utils/debug.js'
import NJU3D from '../components/NJU3D.vue'
import WeatherOverviewPanel from '../components/WeatherOverviewPanel.vue'
import { api } from '../api/index.js'

const log = createLogger('Home')

const emit = defineEmits(['buildingClick', 'ready', 'building-detail'])
const nju3dRef = ref(null)
const drawerCollapsed = ref(true)
const campusWeatherName = ref('多云')
const totalComments = ref(0)
const displayWeather = ref('cloudy')
const sceneReady = ref(false)

// 天气总览
const showOverview = ref(false)
const overviewZones = ref([])

// 选中建筑
const selectedBuilding = ref(null)
const buildingPosts = ref([])

// 情绪统计
const emotionStats = ref([
  { name: '愉悦', percent: 0, color: '#f0a860' },
  { name: '平静', percent: 0, color: '#5ac8a0' },
  { name: '焦虑', percent: 0, color: '#7ec8e3' },
  { name: '低落', percent: 0, color: '#a080d0' },
  { name: '其他', percent: 0, color: '#888888' },
])

// 热门评论
const hotComments = ref([])
const hotTags = ref([])

// ═══════════ 主天气图标 ═══════════
const mainWeatherIcon = computed(() => {
  const icons = {
    sunny: '☀️', cloudy: '⛅', overcast: '☁️',
    rainy: '🌧️', heavy_rain: '⛈️', thunderstorm: '🌩️', snow: '❄️'
  }
  return icons[displayWeather.value] || '⛅'
})

// ═══════════ 情绪统计（圆环图） ═══════════
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

// ═══════════ 选中建筑所属分区数据 ═══════════
const selectedZoneData = computed(() => {
  if (!selectedBuilding.value?.zone) return null
  const zoneId = selectedBuilding.value.zone.id
  return overviewZones.value.find(z => z.id === zoneId) || null
})

// ═══════════ 事件 ═══════════
function onBuildingClick(data) {
  log.log('onBuildingClick', { name: data.displayName || data.name, zone: data.zone, center: data.center })

  if (selectedBuilding.value?.name === data.name) {
    selectedBuilding.value = null
    buildingPosts.value = []
    return
  }

  selectedBuilding.value = data

  // 从 API 获取真实投稿
  loadBuildingPosts(data.name)

  // 收起抽屉
  drawerCollapsed.value = true
}

function onSceneReady() {
  log.log('onSceneReady — 3D场景加载完成')
  sceneReady.value = true
}

function viewBuildingDetail() {
  log.log('viewBuildingDetail', { name: selectedBuilding.value?.name })
  if (selectedBuilding.value?.name) {
    emit('building-detail', selectedBuilding.value.name)
  }
}

// ═══════════ API 调用 ═══════════
async function loadBuildingPosts(buildingName) {
  log.log('loadBuildingPosts', { buildingName })
  try {
    const res = await api.getBuildingPosts(buildingName)
    if (res.code === 0 && res.data) {
      log.log('loadBuildingPosts success', { count: res.data.length })
      buildingPosts.value = res.data.map(p => ({
        id: p.id,
        content: p.content,
        weatherIcon: getWeatherIcon(p.weatherCode),
        time: formatTime(p.createdAt)
      }))
    }
  } catch (e) {
    log.error('加载建筑投稿失败:', e)
    buildingPosts.value = []
  }
}

async function loadMapOverview() {
  log.log('loadMapOverview')
  try {
    const res = await api.getMapOverview()
    if (res.code === 0 && res.data) {
      log.log('loadMapOverview success', { campusWeather: res.data.campusMainWeather, areaCount: res.data.areas?.length })
      totalComments.value = res.data.totalPostsToday || 0

      // 全校主天气
      const cw = res.data.campusMainWeather
      if (cw) {
        campusWeatherName.value = cw.weatherName || '多云'
        displayWeather.value = cw.weatherCode || 'cloudy'
      }

      // 热门标签
      if (res.data.hotTags) {
        hotTags.value = res.data.hotTags.slice(0, 8).map(t => ({ name: t.name }))
      }

      // 最新投稿
      if (res.data.hotComments) {
        hotComments.value = res.data.hotComments.map(p => ({
          id: p.id,
          content: p.content,
          createdAt: p.createdAt,
          authorName: p.authorName || '匿名',
          authorAvatar: p.authorAvatar || '👤',
          buildingName: p.buildingName || '',
          weatherIcon: p.weatherIcon || '',
          tags: p.tags || []
        }))
      }

      // 情绪分布
      if (res.data.emotionDistribution) {
        updateEmotionStats(res.data.emotionDistribution)
      }

      // ★ 用 API 返回的真实天气更新各分区（替代 ZoneData.js 静态数据）
      if (res.data.areas && nju3dRef.value) {
        const weatherMap = {}
        for (const area of res.data.areas) {
          weatherMap[area.id] = area.weatherCode || 'cloudy'
        }
        nju3dRef.value.setAllZoneWeathers(weatherMap)
      }
    }
  } catch (e) {
    log.log('API 未连接，使用默认数据')
  }
}

function updateEmotionStats(distribution) {
  if (!distribution) return
  // 后端返回 {happy: {count: 5, percent: 50}} 或 {happy: 5}，两种格式兼容
  const counts = {}
  let total = 0
  for (const [key, val] of Object.entries(distribution)) {
    const count = typeof val === 'object' && val !== null ? (val.count || 0) : (val || 0)
    counts[key] = count
    total += count
  }
  if (total === 0) return

  const map = {
    happy: { name: '愉悦', color: '#f0a860' },
    calm: { name: '平静', color: '#5ac8a0' },
    anxious: { name: '焦虑', color: '#7ec8e3' },
    sad: { name: '低落', color: '#a080d0' },
  }

  const stats = []
  for (const [key, count] of Object.entries(counts)) {
    const meta = map[key] || { name: key, color: '#888888' }
    stats.push({ name: meta.name, percent: Math.round(count * 100 / total), color: meta.color })
  }
  emotionStats.value = stats
}

function getWeatherIcon(code) {
  const icons = { sunny: '☀️', cloudy: '⛅', overcast: '☁️', rainy: '🌧️', heavy_rain: '⛈️', thunderstorm: '🌩️', snow: '❄️' }
  return icons[code] || '⛅'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return `${Math.floor(diff / 86400000)} 天前`
}

// 定时刷新天气数据（每 60 秒）
let refreshTimer = null

async function loadOverview() {
  log.log('loadOverview')
  try {
    const res = await api.getMapOverview()
    if (res.code === 0 && res.data && res.data.areas) {
      log.log('loadOverview success', { zoneCount: res.data.areas.length })
      overviewZones.value = res.data.areas.map(a => ({
        id: a.id,
        name: a.name,
        weatherCode: a.weatherCode || 'cloudy',
        postCount: a.postCount || 0
      }))
    }
  } catch (e) {
    log.error('加载总览失败:', e)
  }
}

onMounted(() => {
  log.log('onMounted — 首页初始化')
  loadMapOverview()
  loadOverview()
  refreshTimer = setInterval(() => {
    log.log('60s刷新 — loadMapOverview + loadOverview')
    loadMapOverview()
    loadOverview()
  }, 60000)
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
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
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  pointer-events: none;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.overview-btn {
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(10,10,30,0.4);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  pointer-events: auto;
}
.overview-btn:hover { background: rgba(255,255,255,0.15); }

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

.comment-avatar-emoji {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: rgba(255,255,255,0.06);
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

.bp-stats {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 12px;
}

.bp-stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.bp-stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.bp-stat-value {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
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
