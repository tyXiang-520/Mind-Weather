<template>
  <div class="page post-page">
    <Toast ref="toast" />
    <div class="post-container">
      <h2 class="page-title">记录心情</h2>
      <p class="page-desc">写下此刻的感受，它将变成校园里的一片云</p>

      <!-- 分区 + 建筑选择 -->
      <div class="zone-picker">
        <label>选择地点</label>
        <div v-for="zone in zoneGroups" :key="zone.id" class="zone-group">
          <div class="zone-group-label">
            <span class="zone-id-badge">{{ zone.id }}</span>
            {{ zone.name }}
          </div>
          <div class="building-grid">
            <button
              v-for="b in zone.buildings"
              :key="b"
              :class="['building-btn', { selected: selectedBuilding === b }]"
              @click="selectBuilding(b, zone.id)"
            >
              {{ b }}
            </button>
          </div>
        </div>
      </div>

      <!-- 已选建筑 -->
      <div v-if="selectedBuilding" class="selected-info">
        已选：<strong>{{ selectedBuilding }}</strong>
        <span class="selected-zone">{{ selectedZoneId }}区</span>
      </div>

      <!-- 文本输入 -->
      <textarea
        v-model="content"
        placeholder="比如：图书馆自习到崩溃，谁来救救我..."
        maxlength="500"
      ></textarea>
      <div class="char-count">{{ content.length }}/500</div>

      <!-- 匿名开关 -->
      <div class="anon-row">
        <label class="anon-toggle">
          <input type="checkbox" v-model="isAnonymous" />
          <span>匿名投稿</span>
        </label>
      </div>

      <!-- 提交 -->
      <button
        class="btn submit-btn"
        :disabled="!canSubmit || submitting"
        @click="submitPost"
      >
        {{ submitting ? 'AI 分析中...' : '生成天气' }}
      </button>

      <!-- 结果卡片 -->
      <div v-if="result" class="result-card glass-panel">
        <div class="result-header">
          <span class="result-icon">{{ result.weatherIcon }}</span>
          <div>
            <div class="result-weather">{{ result.weatherName }}</div>
            <div class="result-emotion">情绪：{{ result.emotionType }}</div>
          </div>
        </div>
        <div class="result-tags">
          <span v-for="tag in result.tags" :key="tag" class="tag-chip">{{ tag }}</span>
        </div>
        <div class="result-building">📍 {{ selectedBuilding }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createLogger } from '../utils/debug.js'
import Toast from '../components/Toast.vue'
import { api, isLoggedIn, getProfile } from '../api/index.js'

const log = createLogger('PostPage')
import { ZONES, getZoneByBuilding } from '../three/ZoneData.js'

// 整理为前端展示用的分区组
const zoneGroups = ZONES.map(z => ({
  id: z.id,
  name: z.name,
  buildings: z.buildings
}))

const content = ref('')
const selectedBuilding = ref('')
const selectedZoneId = ref('')
const submitting = ref(false)
const result = ref(null)
const isAnonymous = ref(false)
const toast = ref(null)

// 初始化匿名设置
onMounted(() => {
  if (isLoggedIn()) {
    const profile = getProfile()
    isAnonymous.value = profile.defaultAnonymous === true
  }
})

const canSubmit = computed(() =>
  content.value.trim().length > 0 &&
  selectedBuilding.value &&
  isLoggedIn()
)

function selectBuilding(buildingName, zoneId) {
  log.log('selectBuilding', { buildingName, zoneId })
  if (selectedBuilding.value === buildingName) {
    selectedBuilding.value = ''
    selectedZoneId.value = ''
  } else {
    selectedBuilding.value = buildingName
    selectedZoneId.value = zoneId
    result.value = null
  }
}

async function submitPost() {
  if (!canSubmit.value) return
  log.log('submitPost 开始', { content: content.value.trim().slice(0,30), building: selectedBuilding.value, zone: selectedZoneId.value, anonymous: isAnonymous.value })
  submitting.value = true
  result.value = null
  try {
    const res = await api.submitPost(
      content.value.trim(),
      selectedBuilding.value,
      selectedZoneId.value,
      isAnonymous.value
    )
    log.log('投稿返回', { code: res.code, weather: res.data?.weatherName, emotion: res.data?.emotionType })
    if (res.code === 0 && res.data) {
      result.value = res.data
      content.value = ''
    } else if (res.code === 401) {
      toast.value?.show('请先登录后再投稿', 'warning')
    } else {
      toast.value?.show(res.message || '投稿失败', 'error')
    }
  } catch (e) {
    toast.value?.show('网络错误，请稍后重试', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.post-page {
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a2e 100%);
  overflow-y: auto;
}

.post-container {
  padding: 24px 16px 100px;
  max-width: 520px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.page-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 20px;
}

.zone-picker {
  margin-bottom: 16px;
}

.zone-picker > label {
  font-size: 12px;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 10px;
}

.zone-group {
  margin-bottom: 10px;
}

.zone-group-label {
  font-size: 12px;
  color: var(--accent);
  margin-bottom: 5px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.zone-id-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(126, 200, 227, 0.2);
  border: 1px solid rgba(126, 200, 227, 0.3);
  font-size: 10px;
  color: #7ec8e3;
}

.building-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.building-btn {
  padding: 6px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.building-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}

.building-btn.selected {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.selected-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
}

.selected-zone {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(126, 200, 227, 0.15);
  font-size: 11px;
  color: #7ec8e3;
}

.char-count {
  text-align: right;
  font-size: 11px;
  color: var(--text-secondary);
  margin: 4px 0 14px;
}

.anon-row {
  margin-bottom: 14px;
}

.anon-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
}

.anon-toggle input {
  width: auto;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
}

.submit-btn:disabled {
  opacity: 0.4;
}

.result-card {
  margin-top: 20px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.result-icon { font-size: 40px; }
.result-weather { font-size: 18px; font-weight: 600; }
.result-emotion { font-size: 12px; color: var(--text-secondary); }

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag-chip {
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

.result-building {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}
</style>
