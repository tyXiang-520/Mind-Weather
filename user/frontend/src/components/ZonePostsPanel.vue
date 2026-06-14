<template>
  <div class="zone-posts-panel" :class="{ visible: show }">
    <div class="zpp-inner">
      <div class="zpp-header">
        <div class="zpp-title">
          <span class="zpp-zone-id">{{ zone?.id }}区</span>
          <span class="zpp-zone-name">{{ zone?.name }}</span>
        </div>
        <button class="zpp-close" @click="$emit('close')">✕</button>
      </div>

      <div v-if="errorMsg" class="zpp-error">{{ errorMsg }}</div>

      <div class="zpp-stats">
        <span class="zpp-weather">{{ getWeatherIcon(zone?.weatherCode) }} {{ getWeatherName(zone?.weatherCode) }}</span>
        <span class="zpp-count">{{ posts.length }} 篇帖子</span>
      </div>

      <div class="zpp-posts">
        <div v-if="loading" class="zpp-loading">加载中...</div>
        <div v-else-if="posts.length === 0" class="zpp-empty">这里还没有人分享心情</div>
        <div v-else v-for="post in posts" :key="post.id" class="zpp-post">
          <div class="zpp-post-header">
            <span class="zpp-post-author-avatar">{{ post.authorAvatar || '👤' }}</span>
            <span class="zpp-post-author-name">{{ post.authorName || '匿名' }}</span>
            <span class="zpp-post-weather">{{ getWeatherIcon(post.weatherCode) }}</span>
            <span class="zpp-post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
          <div class="zpp-post-content">{{ post.content }}</div>
          <div class="zpp-post-footer">
            <span class="zpp-post-building">{{ post.buildingName || '未知位置' }}</span>
            <div class="zpp-post-actions">
              <button class="zpp-action-btn" :class="{ active: post.liked }" @click="toggleLike(post)">
                {{ post.liked ? '❤️' : '🤍' }} {{ post.likeCount || 0 }}
              </button>
              <button class="zpp-action-btn" @click="toggleComments(post)">
                💬 {{ post.commentCount || 0 }}
              </button>
            </div>
          </div>

          <!-- 评论区域 -->
          <div v-if="post.showComments" class="zpp-comments">
            <div v-if="post.commentsLoading" class="zpp-loading">加载评论中...</div>
            <template v-else>
              <div v-for="comment in post.comments" :key="comment.id" class="zpp-comment">
                <span class="zpp-comment-text">{{ comment.content }}</span>
                <span class="zpp-comment-time">{{ formatTime(comment.createdAt) }}</span>
              </div>
              <div v-if="post.comments.length === 0" class="zpp-no-comments">暂无评论</div>
            </template>

            <!-- 评论输入 -->
            <div class="zpp-comment-input">
              <input
                v-model="commentText"
                placeholder="写评论..."
                @keyup.enter="submitComment(post)"
              />
              <button class="zpp-send-btn" @click="submitComment(post)">发送</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { api } from '../api/index.js'

const props = defineProps({
  show: { type: Boolean, default: false },
  zone: { type: Object, default: null }
})

defineEmits(['close'])

const posts = ref([])
const loading = ref(false)
const commentText = ref('')
const errorMsg = ref('')

function getToken() {
  return localStorage.getItem('token') || ''
}

function checkLogin() {
  if (!getToken()) {
    errorMsg.value = '请先登录后再操作'
    setTimeout(() => errorMsg.value = '', 3000)
    return false
  }
  return true
}

const WEATHER_META = {
  sunny:        { icon: '☀️', name: '晴' },
  cloudy:       { icon: '⛅', name: '多云' },
  overcast:     { icon: '☁️', name: '阴' },
  rainy:        { icon: '🌧️', name: '小雨' },
  heavy_rain:   { icon: '⛈️', name: '暴雨' },
  thunderstorm: { icon: '🌩️', name: '雷暴' },
  snow:         { icon: '❄️', name: '雪' },
}

function getWeatherIcon(code) {
  return (WEATHER_META[code] || WEATHER_META.cloudy).icon
}

function getWeatherName(code) {
  return (WEATHER_META[code] || WEATHER_META.cloudy).name
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

async function loadPosts() {
  if (!props.zone?.id) return
  loading.value = true
  try {
    const res = await api.getZonePosts(props.zone.id)
    if (res.code === 0 && res.data) {
      posts.value = res.data.map(p => ({
        id: p.postId,
        content: p.content,
        buildingName: p.buildingName,
        zoneId: p.zoneId,
        weatherCode: p.weatherCode,
        emotionLabel: p.emotionType,
        createdAt: p.createdAt,
        authorName: p.authorName || '匿名',
        authorAvatar: p.authorAvatar || '',
        liked: false,
        likeCount: p.likeCount || 0,
        commentCount: p.commentCount || 0,
        comments: [],
        showComments: false,
        commentsLoading: false
      }))
      // 批量查点赞状态
      if (getToken()) {
        await checkLikeStatus()
      }
    }
  } catch (e) {
    console.error('加载分区帖子失败:', e)
  } finally {
    loading.value = false
  }
}

async function checkLikeStatus() {
  for (const post of posts.value) {
    try {
      const res = await api.getLikeStatus(post.id)
      if (res.code === 0 && res.data) {
        post.liked = res.data.liked || false
        post.likeCount = res.data.likeCount ?? post.likeCount
      }
    } catch (e) { /* ignore */ }
  }
}

async function toggleLike(post) {
  if (!checkLogin()) return
  try {
    const res = await api.toggleLike(post.id)
    if (res.code === 0) {
      post.liked = !post.liked
      post.likeCount = post.liked ? (post.likeCount || 0) + 1 : Math.max(0, (post.likeCount || 0) - 1)
    } else {
      errorMsg.value = res.message || '点赞失败'
      setTimeout(() => errorMsg.value = '', 3000)
    }
  } catch (e) {
    console.error('点赞失败:', e)
  }
}

async function toggleComments(post) {
  if (post.showComments) {
    post.showComments = false
    return
  }
  post.showComments = true
  await loadComments(post)
}

async function loadComments(post) {
  post.commentsLoading = true
  try {
    const res = await api.getComments(post.id)
    if (res.code === 0 && res.data) {
      const commentList = res.data.comments || res.data
      post.comments = Array.isArray(commentList) ? commentList : []
      post.commentCount = post.comments.length
    }
  } catch (e) {
    console.error('加载评论失败:', e)
  } finally {
    post.commentsLoading = false
  }
}

async function submitComment(post) {
  if (!commentText.value.trim()) return
  if (!checkLogin()) return
  try {
    const res = await api.addComment(post.id, commentText.value.trim())
    if (res.code === 0) {
      commentText.value = ''
      await loadComments(post)
    } else {
      errorMsg.value = res.message || '评论失败'
      setTimeout(() => errorMsg.value = '', 3000)
    }
  } catch (e) {
    console.error('评论失败:', e)
  }
}

watch(() => props.show, (val) => {
  if (val && props.zone?.id) {
    loadPosts()
  }
})

watch(() => props.zone, (val) => {
  if (val?.id && props.show) {
    loadPosts()
  }
})
</script>

<style scoped>
.zone-posts-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s;
}

.zone-posts-panel.visible {
  opacity: 1;
  pointer-events: auto;
}

.zpp-inner {
  width: 90%;
  max-width: 420px;
  max-height: 85vh;
  overflow-y: auto;
  background: rgba(14, 14, 40, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 16px;
}

.zpp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.zpp-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.zpp-zone-id {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.zpp-zone-name {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.zpp-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
}

.zpp-error {
  background: rgba(232, 85, 85, 0.15);
  border: 1px solid rgba(232, 85, 85, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
  color: #e85555;
  font-size: 12px;
  text-align: center;
}

.zpp-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.zpp-loading, .zpp-empty {
  text-align: center;
  padding: 30px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.zpp-post {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 8px;
}

.zpp-post-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
}

.zpp-post-author-avatar {
  font-size: 16px;
}

.zpp-post-author-name {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.zpp-post-weather {
  font-size: 14px;
  margin-left: auto;
}

.zpp-post-emotion {
  color: rgba(255, 255, 255, 0.7);
}

.zpp-post-time {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.35);
  font-size: 11px;
}

.zpp-post-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  margin-bottom: 8px;
}

.zpp-post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.zpp-post-building {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.zpp-post-actions {
  display: flex;
  gap: 8px;
}

.zpp-action-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.2s;
}

.zpp-action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.zpp-action-btn.active {
  color: #ff6b6b;
}

.zpp-comments {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  max-height: 200px;
  overflow-y: auto;
}

.zpp-comment {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  padding: 6px 0;
}

.zpp-comment-text {
  color: rgba(255, 255, 255, 0.7);
}

.zpp-comment-time {
  color: rgba(255, 255, 255, 0.3);
  font-size: 11px;
  flex-shrink: 0;
}

.zpp-no-comments {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  padding: 10px;
}

.zpp-comment-input {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.zpp-comment-input input {
  flex: 1;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 8px 10px;
  color: #fff;
  font-size: 13px;
  outline: none;
}

.zpp-comment-input input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.zpp-send-btn {
  background: rgba(80, 180, 130, 0.6);
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.zpp-send-btn:hover {
  background: rgba(80, 180, 130, 0.8);
}
</style>
