<template>
  <div class="overview-panel" :class="{ visible: show }">
    <div class="overview-inner">
      <div class="overview-header">
        <span class="overview-title">🗺️ 天气总览</span>
        <button class="overview-close" @click="$emit('close')">✕</button>
      </div>
      <div class="overview-grid">
        <div
          v-for="zone in zones"
          :key="zone.id"
          class="overview-zone"
          :class="{ clickable: zone.postCount > 0 }"
          :style="{ borderColor: getWeatherColor(zone.weatherCode) }"
          @click="onZoneClick(zone)"
        >
          <div class="oz-header">
            <span class="oz-id">{{ zone.id }}区</span>
            <span class="oz-icon">{{ getWeatherIcon(zone.weatherCode) }}</span>
          </div>
          <div class="oz-name">{{ zone.name }}</div>
          <div class="oz-weather" :style="{ color: getWeatherColor(zone.weatherCode) }">
            {{ getWeatherName(zone.weatherCode) }}
          </div>
          <div class="oz-posts">{{ zone.postCount || 0 }} 篇</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 分区帖子详情 -->
  <ZonePostsPanel
    :show="showZonePosts"
    :zone="selectedZone"
    @close="showZonePosts = false"
  />
</template>

<script setup>
import { ref } from 'vue'
import { createLogger } from '../utils/debug.js'
import ZonePostsPanel from './ZonePostsPanel.vue'

const log = createLogger('WeatherOverview')

const props = defineProps({
  show: { type: Boolean, default: false },
  zones: { type: Array, default: () => [] }
})

defineEmits(['close'])

const showZonePosts = ref(false)
const selectedZone = ref(null)

const WEATHER_META = {
  sunny:        { icon: '☀️', name: '晴', color: '#f0c040' },
  cloudy:       { icon: '⛅', name: '多云', color: '#cccccc' },
  overcast:     { icon: '☁️', name: '阴', color: '#888888' },
  rainy:        { icon: '🌧️', name: '小雨', color: '#5588aa' },
  heavy_rain:   { icon: '⛈️', name: '暴雨', color: '#3366aa' },
  thunderstorm: { icon: '🌩️', name: '雷暴', color: '#6644aa' },
  snow:         { icon: '❄️', name: '雪', color: '#e8eeff' },
}

function getWeatherIcon(code) {
  return (WEATHER_META[code] || WEATHER_META.cloudy).icon
}

function getWeatherName(code) {
  return (WEATHER_META[code] || WEATHER_META.cloudy).name
}

function getWeatherColor(code) {
  return (WEATHER_META[code] || WEATHER_META.cloudy).color
}

function onZoneClick(zone) {
  log.log('onZoneClick', { id: zone.id, name: zone.name, weather: zone.weatherCode, posts: zone.postCount })
  if (!zone.postCount || zone.postCount === 0) {
    log.log('该分区无帖子，不展开')
    return
  }
  selectedZone.value = zone
  showZonePosts.value = true
}
</script>

<style scoped>
.overview-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s;
}

.overview-panel.visible {
  opacity: 1;
  pointer-events: auto;
}

.overview-inner {
  width: 90%;
  max-width: 420px;
  max-height: 80vh;
  overflow-y: auto;
  background: rgba(14, 14, 40, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 16px;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.overview-title {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.overview-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.overview-zone {
  background: rgba(255, 255, 255, 0.05);
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 10px 8px;
  text-align: center;
  transition: all 0.2s;
}

.overview-zone.clickable {
  cursor: pointer;
}

.overview-zone.clickable:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.03);
}

.oz-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.oz-id {
  font-size: 11px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.5);
}

.oz-icon {
  font-size: 18px;
}

.oz-name {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 2px;
}

.oz-weather {
  font-size: 12px;
  font-weight: 600;
}

.oz-posts {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 2px;
}
</style>
