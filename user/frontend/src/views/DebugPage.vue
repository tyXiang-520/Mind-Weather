<template>
  <div class="debug-page">
    <NJU3D
      ref="nju3dRef"
      class="scene-layer"
      :debug="true"
      @building-click="onBuildingClick"
      @ready="onSceneReady"
      @zoneWeatherChange="onZoneWeatherChange"
    />

    <!-- Top bar -->
    <div class="top-bar">
      <div class="logo">
        <div class="logo-icon">🔧</div>
        <div class="logo-text">
          <div class="logo-main">MindWeather Debug</div>
          <div class="logo-sub">右侧面板切换天气 · 底部查看日志</div>
        </div>
      </div>
      <a class="back-link" href="/">返回正式页面</a>
    </div>

    <!-- Debug 日志面板 -->
    <div class="debug-console">
      <div class="dc-header">
        <span class="dc-title">Debug Console</span>
        <button class="dc-clear" @click="logs = []">清空</button>
      </div>
      <div class="dc-logs" ref="logsRef">
        <div v-for="(log, i) in logs" :key="i" class="dc-log" :class="log.type">
          <span class="dc-time">{{ log.time }}</span>
          <span class="dc-msg">{{ log.msg }}</span>
        </div>
        <div v-if="logs.length === 0" class="dc-empty">等待事件...</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import NJU3D from '../components/NJU3D.vue'

const nju3dRef = ref(null)
const logs = ref([])
const logsRef = ref(null)

function addLog(msg, type = 'info') {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  logs.value.push({ time, msg, type })
  if (logs.value.length > 100) logs.value.shift()
  nextTick(() => {
    if (logsRef.value) logsRef.value.scrollTop = logsRef.value.scrollHeight
  })
}

function onBuildingClick(data) {
  addLog(`点击建筑: ${data.displayName || data.name} (${data.zone?.id}区)`, 'event')
}

function onSceneReady() {
  addLog('3D 场景加载完成', 'ok')
}

function onZoneWeatherChange({ zoneId, weather }) {
  addLog(`${zoneId}区天气 → ${weather}`, 'action')
}

addLog('Debug 模式启动', 'ok')
</script>

<style scoped>
.debug-page {
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
  top: 12px;
  left: 0;
  right: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  pointer-events: none;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 100, 50, 0.3);
  backdrop-filter: blur(12px);
  padding: 8px 18px;
  border-radius: 24px;
  border: 1px solid rgba(255, 100, 50, 0.3);
}

.logo-icon { font-size: 22px; }

.logo-main {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
}

.logo-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.55);
}

.back-link {
  pointer-events: auto;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.1);
  padding: 6px 14px;
  border-radius: 16px;
}
.back-link:hover { background: rgba(255, 255, 255, 0.2); }

/* ── Debug Console ── */
.debug-console {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 30;
  height: 140px;
  background: rgba(5, 5, 20, 0.92);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
}

.dc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.dc-title {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
}

.dc-clear {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  font-size: 11px;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 6px;
}
.dc-clear:hover { background: rgba(255, 255, 255, 0.08); }

.dc-logs {
  flex: 1;
  overflow-y: auto;
  padding: 6px 14px;
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  font-size: 12px;
}

.dc-log {
  display: flex;
  gap: 10px;
  padding: 2px 0;
  line-height: 1.5;
}

.dc-time {
  color: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

.dc-msg { color: rgba(255, 255, 255, 0.7); }

.dc-log.ok .dc-msg { color: #5ac8a0; }
.dc-log.event .dc-msg { color: #7ec8e3; }
.dc-log.action .dc-msg { color: #f0a860; }
.dc-log.error .dc-msg { color: #e85555; }

.dc-empty {
  color: rgba(255, 255, 255, 0.2);
  text-align: center;
  padding: 20px;
}
</style>
