<template>
  <div class="comment-section">
    <div class="comment-title">评论 ({{ total }})</div>

    <!-- Add comment -->
    <div class="add-comment" v-if="canComment">
      <input
        v-model="newComment"
        placeholder="写下评论..."
        maxlength="200"
        @keyup.enter="submitComment"
      />
      <button class="comment-submit" :disabled="!newComment.trim()" @click="submitComment">发送</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="comment-loading">加载中...</div>

    <!-- Empty -->
    <div v-else-if="!comments.length" class="comment-empty">暂无评论</div>

    <!-- Comment list -->
    <div v-for="c in comments" :key="c.id" class="comment-item">
      <div class="comment-body">
        <span class="comment-author">用户 {{ c.userId }}</span>
        <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
      </div>
      <div class="comment-text">{{ c.content }}</div>
      <button
        v-if="showDelete && c.userId === currentUserId"
        class="comment-del"
        @click="onDelete(c.id)"
      >删除</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createLogger } from '../utils/debug.js'

const log = createLogger('CommentList')

const props = defineProps({
  postId: { type: [Number, String], required: true },
  canComment: { type: Boolean, default: false },
  showDelete: { type: Boolean, default: false },
  currentUserId: { type: Number, default: null }
})

const emit = defineEmits(['load', 'submit', 'delete'])

const comments = ref([])
const total = ref(0)
const loading = ref(true)
const newComment = ref('')

function formatTime(timeStr) {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return timeStr }
}

async function loadComments() {
  loading.value = true
  log.log('loadComments', { postId: props.postId })
  emit('load', (data) => {
    if (data) {
      comments.value = data.comments || []
      total.value = comments.value.length
      log.log('加载完成', { total: total.value })
    }
    loading.value = false
  })
}

async function submitComment() {
  if (!newComment.value.trim()) return
  log.log('submitComment', { postId: props.postId, content: newComment.value.trim().slice(0,20) })
  emit('submit', newComment.value.trim(), (ok) => {
    if (ok) {
      log.log('评论提交成功')
      newComment.value = ''
      loadComments()
    } else {
      log.log('评论提交失败')
    }
  })
}

function onDelete(commentId) {
  log.log('onDelete', { commentId })
  emit('delete', commentId, (ok) => {
    if (ok) {
      log.log('评论删除成功')
      loadComments()
    }
  })
}

onMounted(loadComments)
</script>

<style scoped>
.comment-section {
  margin-top: 12px;
}
.comment-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: rgba(255,255,255,0.7);
}

.add-comment {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.add-comment input { flex: 1; min-height: 36px; font-size: 13px; }
.comment-submit {
  padding: 8px 16px;
  background: var(--accent);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.comment-submit:disabled { opacity: 0.4; }

.comment-loading, .comment-empty {
  text-align: center;
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  padding: 12px 0;
}

.comment-item {
  padding: 8px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.comment-item:last-child { border-bottom: none; }

.comment-body {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}
.comment-author { font-size: 11px; color: var(--accent); font-weight: 500; }
.comment-time { font-size: 10px; color: rgba(255,255,255,0.3); }
.comment-text { font-size: 13px; line-height: 1.4; }

.comment-del {
  margin-top: 4px;
  background: none;
  border: none;
  color: #ff6b6b;
  font-size: 11px;
  cursor: pointer;
}
</style>
