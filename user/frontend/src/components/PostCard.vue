<template>
  <div class="post-card glass-panel" @click="onClick">
    <div class="post-header">
      <span class="post-author-avatar">{{ post.authorAvatar || '👤' }}</span>
      <span class="post-author-name">{{ post.authorName || '匿名' }}</span>
      <WeatherIcon :code="post.weatherCode" size="sm" />
      <div class="post-header-info">
        <span class="post-weather-name">{{ post.weatherName }}</span>
        <span class="post-emotion">{{ post.emotionType }}</span>
      </div>
      <button v-if="showDelete && canDelete" class="delete-btn" @click.stop="onDelete">✕</button>
    </div>

    <div class="post-content">{{ post.content }}</div>

    <div class="post-tags" v-if="post.tags && post.tags.length">
      <span v-for="tag in post.tags" :key="tag" class="tag-chip">{{ tag }}</span>
    </div>

    <div class="post-footer">
      <span class="post-building">{{ post.buildingName }}</span>
      <span class="post-time">{{ formatTime(post.createdAt) }}</span>
    </div>

    <!-- Comment & Like bar -->
    <div class="post-actions" v-if="showActions">
      <button class="action-btn" @click.stop="onComment">
        <span class="action-icon">💬</span>
        <span>{{ commentCount }}</span>
      </button>
      <button class="action-btn" :class="{ liked: liked }" @click.stop="onLike">
        <span class="action-icon">{{ liked ? '❤️' : '🤍' }}</span>
        <span>{{ likeCount }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { createLogger } from '../utils/debug.js'
import WeatherIcon from './WeatherIcon.vue'

const log = createLogger('PostCard')

const props = defineProps({
  post: { type: Object, required: true },
  showDelete: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  showActions: { type: Boolean, default: false },
  liked: { type: Boolean, default: false },
  likeCount: { type: Number, default: 0 },
  commentCount: { type: Number, default: 0 }
})

const emit = defineEmits(['click', 'delete', 'comment', 'like'])

function onClick() { log.log('onClick', { postId: props.post.postId, building: props.post.buildingName }); emit('click') }
function onDelete() { log.log('onDelete', { postId: props.post.postId }); emit('delete') }
function onComment() { log.log('onComment', { postId: props.post.postId }); emit('comment') }
function onLike() { log.log('onLike', { postId: props.post.postId, liked: props.liked, count: props.likeCount }); emit('like') }

function formatTime(timeStr) {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    if (isNaN(d.getTime())) return ''
    const now = new Date()
    const diff = now - d
    if (diff < 0) return '刚刚'
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
    if (diff < 172800000) return '昨天'
    return `${d.getMonth() + 1}/${d.getDate()}`
  } catch { return '' }
}
</script>

<style scoped>
.post-card {
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.15s;
}
.post-card:active { transform: scale(0.98); }

.post-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.post-author-avatar {
  font-size: 18px;
}

.post-author-name {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255,255,255,0.7);
}

.post-header-info {
  display: flex;
  gap: 6px;
  align-items: center;
  flex: 1;
}
.post-weather-name { font-size: 13px; font-weight: 600; }
.post-emotion {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.06);
  padding: 1px 8px;
  border-radius: 8px;
}

.delete-btn {
  width: 24px; height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(255,80,80,0.15);
  color: #ff6b6b;
  font-size: 12px;
  cursor: pointer;
}

.post-content {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 6px;
  word-break: break-word;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.tag-chip {
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(126,200,227,0.12);
  font-size: 10px;
  color: rgba(255,255,255,0.6);
}

.post-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: rgba(255,255,255,0.4);
}

.post-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: rgba(255,255,255,0.5);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
}
.action-btn.liked { color: #ff6b6b; }
.action-icon { font-size: 14px; }
</style>
