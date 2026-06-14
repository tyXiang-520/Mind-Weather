<template>
  <span class="weather-icon" :class="[code, size]" :title="name">{{ icon }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  code: { type: String, default: 'cloudy' },
  size: { type: String, default: 'md' }
})

const WEATHER_MAP = {
  sunny: { icon: '☀️', name: '晴' },
  cloudy: { icon: '⛅', name: '多云' },
  overcast: { icon: '☁️', name: '阴' },
  rainy: { icon: '🌧️', name: '小雨' },
  heavy_rain: { icon: '⛈️', name: '暴雨' },
  thunderstorm: { icon: '🌩️', name: '雷暴' }
}

const icon = computed(() => WEATHER_MAP[props.code]?.icon || '⛅')
const name = computed(() => WEATHER_MAP[props.code]?.name || '多云')
</script>

<style scoped>
.weather-icon { display: inline-block; line-height: 1; }
.weather-icon.sm { font-size: 20px; }
.weather-icon.md { font-size: 32px; }
.weather-icon.lg { font-size: 48px; }
.weather-icon.xl { font-size: 64px; }
</style>
