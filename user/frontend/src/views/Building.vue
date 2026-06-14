<template>
  <div class="page building-page">
    <Toast ref="toast" />
    <div class="building-container">
      <!-- Back button -->
      <button class="back-btn" @click="goBack">← 返回</button>

      <!-- Building header -->
      <div class="building-header">
        <h2>{{ displayName }}</h2>
        <span class="zone-tag">{{ zoneId }}区</span>
      </div>

      <!-- Weather distribution for this building's zone -->
      <div class="zone-weather glass-panel" v-if="zoneWeather.weatherDistribution">
        <div class="zw-title">分区天气分布</div>
        <div class="zw-items">
          <div v-for="(v, k) in zoneWeather.weatherDistribution" :key="k" class="zw-item">
            <span class="zw-icon">{{ v.icon }}</span>
            <span class="zw-name">{{ v.name }}</span>
            <span class="zw-count">{{ v.count }}篇</span>
          </div>
        </div>
      </div>

      <!-- Posts -->
      <div class="section-title">心情动态</div>
      <div v-if="loading" class="loading-text">加载中...</div>
      <div v-else-if="!posts.length" class="empty-text">这里还没有心情记录</div>
      <PostCard
        v-for="p in posts"
        :key="p.postId"
        :post="p"
        :showActions="true"
        :liked="likedMap[p.postId]"
        :likeCount="likeCountMap[p.postId] || 0"
        :commentCount="commentCountMap[p.postId] || 0"
        @click="selectPost(p)"
        @like="toggleLike(p.postId)"
        @comment="selectPost(p)"
        @delete="deletePost(p.postId)"
      />

      <!-- Post detail modal -->
      <div v-if="selectedPost" class="modal-overlay" @click.self="selectedPost = null">
        <div class="modal-content glass-panel">
          <button class="modal-close" @click="selectedPost = null">✕</button>
          <PostCard
            :post="selectedPost"
            :showActions="false"
          />
          <CommentList
            :postId="selectedPost.postId"
            :canComment="!!token"
            :showDelete="true"
            :currentUserId="userId"
            @load="onLoadComments"
            @submit="onSubmitComment"
            @delete="onDeleteComment"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { createLogger } from '../utils/debug.js'
import { api, isLoggedIn } from '../api/index.js'
import PostCard from '../components/PostCard.vue'
import CommentList from '../components/CommentList.vue'
import Toast from '../components/Toast.vue'
import { getZoneByBuilding, getAllZones } from '../three/ZoneData.js'

const log = createLogger('Building')

const props = defineProps({
  buildingName: { type: String, default: '' }
})
const emit = defineEmits(['back'])

const token = localStorage.getItem('token') || ''
const userId = ref(null)
const posts = ref([])
const loading = ref(true)
const selectedPost = ref(null)
const likedMap = ref({})
const likeCountMap = ref({})
const commentCountMap = ref({})
const zoneWeather = ref({})
const toast = ref(null)

const zone = computed(() => getZoneByBuilding(props.buildingName))
const zoneId = computed(() => zone.value?.id || '')
const displayName = computed(() => {
  // 去掉后缀，如 _Ⅰ区 等
  const name = props.buildingName
  return name
})

function goBack() { log.log('goBack'); emit('back') }

async function loadData() {
  loading.value = true
  log.log('loadData', { buildingName: props.buildingName, zoneId: zoneId.value })
  try {
    const res = await api.getBuildingPosts(props.buildingName)
    if (res.code === 0) {
      posts.value = res.data || []
      log.log('获取帖子', { count: posts.value.length })
    }
  } catch (e) { log.error('获取帖子异常', e) }
  try {
    const zRes = await api.getWeatherDistribution(zoneId.value)
    if (zRes.code === 0) zoneWeather.value = zRes.data || {}
  } catch { log.log('获取天气分布失败（可能无数据）') }
  // Load like/comment counts if logged in
  if (token) {
    const profileRes = await api.profile()
    if (profileRes.code === 0) userId.value = profileRes.data.userId
    for (const p of posts.value) {
      commentCountMap.value[p.postId] = p.commentCount || 0
      try {
        const lr = await api.getLikeStatus(p.postId)
        if (lr.code === 0) {
          likedMap.value[p.postId] = lr.data.liked
          likeCountMap.value[p.postId] = lr.data.likeCount
        }
      } catch { log.log('获取点赞状态失败') }
    }
  } else {
    // 未登录时使用帖子自带的计数
    for (const p of posts.value) {
      likeCountMap.value[p.postId] = p.likeCount || 0
      commentCountMap.value[p.postId] = p.commentCount || 0
    }
  }
  loading.value = false
}

async function toggleLike(postId) {
  if (!token) { toast.value?.show('请先登录', 'warning'); return }
  log.log('toggleLike', { postId })
  const res = await api.toggleLike(postId)
  if (res.code === 0) {
    log.log('点赞成功', { liked: res.data.liked, count: res.data.likeCount })
    likedMap.value[postId] = res.data.liked
    likeCountMap.value[postId] = res.data.likeCount
  }
}

function selectPost(post) {
  log.log('selectPost', { postId: post.postId })
  selectedPost.value = post
}

async function deletePost(postId) {
  if (!confirm('确定删除这条投稿？')) return
  log.log('deletePost', { postId })
  const res = await api.deletePost(postId)
  if (res.code === 0) {
    posts.value = posts.value.filter(p => p.postId !== postId)
    log.log('删除成功')
  } else {
    toast.value?.show(res.message || '删除失败', 'error')
  }
}

// Comment callbacks
function onLoadComments(callback) {
  if (!selectedPost.value) { callback(null); return }
  log.log('onLoadComments', { postId: selectedPost.value.postId })
  api.getComments(selectedPost.value.postId).then(res => {
    callback(res.code === 0 ? res.data : { comments: [] })
  })
}
function onSubmitComment(content, callback) {
  if (!selectedPost.value) { callback(false); return }
  log.log('onSubmitComment', { postId: selectedPost.value.postId, content: content.slice(0,20) })
  api.addComment(selectedPost.value.postId, content).then(res => {
    callback(res.code === 0)
  })
}
function onDeleteComment(commentId, callback) {
  log.log('onDeleteComment', { commentId })
  api.deleteComment(commentId).then(res => {
    callback(res.code === 0)
  })
}

onMounted(loadData)
</script>

<style scoped>
.building-page {
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a2e 100%);
  overflow-y: auto;
}
.building-container {
  padding: 16px 16px 80px;
  max-width: 520px;
  margin: 0 auto;
}

.back-btn {
  background: none;
  border: none;
  color: var(--accent);
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
  margin-bottom: 8px;
}

.building-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.building-header h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}
.zone-tag {
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(126,200,227,0.15);
  font-size: 11px;
  color: var(--accent);
}

.zone-weather { margin-bottom: 16px; }
.zw-title { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 8px; }
.zw-items { display: flex; flex-wrap: wrap; gap: 8px; }
.zw-item {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 10px;
  border-radius: 10px;
  background: rgba(255,255,255,0.04);
}
.zw-icon { font-size: 16px; }
.zw-name { font-size: 11px; color: rgba(255,255,255,0.6); }
.zw-count { font-size: 10px; color: rgba(255,255,255,0.3); }

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 16px 0 10px;
}

.loading-text, .empty-text {
  text-align: center;
  padding: 40px 0;
  color: rgba(255,255,255,0.4);
  font-size: 13px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  z-index: 200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.modal-content {
  width: 100%;
  max-width: 500px;
  max-height: 75vh;
  overflow-y: auto;
  border-radius: 20px 20px 0 0;
  padding: 20px 20px 80px;
  position: relative;
  background: rgba(15, 15, 35, 0.95);
}
.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px; height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.08);
  color: #fff;
  cursor: pointer;
  z-index: 10;
}
</style>
