<template>
  <div class="page profile-page">
    <Toast ref="toast" />
    <div class="profile-container">
      <!-- Not logged in -->
      <div v-if="!loggedIn" class="auth-section">
        <div class="auth-icon">🔐</div>
        <h2>欢迎来到心晴</h2>
        <p>登录后查看你的个人气象空间</p>
        <div class="auth-form">
          <input v-model="email" type="email" placeholder="邮箱" />
          <input v-model="password" type="password" placeholder="密码" />
          <div class="auth-btns">
            <button class="btn" @click="doLogin">登录</button>
            <button class="btn btn-secondary" @click="switchToRegister">注册</button>
          </div>
        </div>
      </div>

      <!-- Logged in -->
      <template v-else>
        <!-- Profile header -->
        <div class="profile-header">
          <span class="profile-avatar">{{ getAvatarUrl() }}</span>
          <div class="profile-info">
            <div class="profile-nickname">{{ profile.nickname || '用户' }}</div>
            <div class="profile-email">{{ profile.email }}</div>
          </div>
          <button class="logout-btn" @click="doLogout">退出</button>
        </div>

        <!-- Settings -->
        <div class="settings-section">
          <div class="settings-title" @click="showSettings = !showSettings">
            ⚙️ 设置 <span class="settings-arrow">{{ showSettings ? '▾' : '▸' }}</span>
          </div>
          <div v-if="showSettings" class="settings-body">
            <div class="setting-row">
              <label>昵称</label>
              <input v-model="editNickname" placeholder="新昵称" maxlength="12" class="setting-input" />
            </div>
            <div class="setting-row">
              <label>头像</label>
              <div class="avatar-picker">
                <span v-for="a in avatarOptions" :key="a" class="avatar-opt" :class="{picked: editAvatar===a}" @click="editAvatar=a">{{ a }}</span>
              </div>
            </div>
            <div class="setting-row">
              <label>默认匿名</label>
              <label class="anon-toggle">
                <input type="checkbox" v-model="editAnon" /> 投稿默认匿名
              </label>
            </div>
            <div class="setting-row">
              <label>密码</label>
              <button class="btn-sm btn-outline" @click="showPwdModal = true">修改密码</button>
            </div>
            <button class="btn save-settings-btn" @click="saveAllSettings">保存设置</button>
            <div v-if="saveMsg" class="save-msg">{{ saveMsg }}</div>
          </div>
        </div>

        <!-- Password change modal -->
        <div class="modal-overlay" v-if="showPwdModal" @click.self="showPwdModal = false">
          <div class="modal-card">
            <div class="modal-title">修改密码</div>
            <input v-model="oldPassword" type="password" placeholder="原密码" />
            <input v-model="newPassword" type="password" placeholder="新密码（至少6位）" />
            <input v-model="confirmPassword" type="password" placeholder="确认新密码" />
            <div class="modal-btns">
              <button class="btn" :disabled="pwdSubmitting" @click="changePassword">确认修改</button>
              <button class="btn btn-secondary" @click="showPwdModal = false">取消</button>
            </div>
            <div v-if="pwdMsg" :class="['pwd-msg', pwdOk ? 'ok' : 'err']">{{ pwdMsg }}</div>
          </div>
        </div>

        <!-- Today weather card -->
        <div class="today-card glass-panel" v-if="todayWeather.weatherCode">
          <div class="today-icon">{{ todayWeather.weatherIcon || '⛅' }}</div>
          <div class="today-info">
            <div class="today-label">今日心情天气</div>
            <div class="today-name">{{ todayWeather.weatherName || '多云' }}</div>
            <div class="today-summary">{{ todayWeather.summary || '今天还没有记录心情' }}</div>
          </div>
        </div>

        <!-- Stats grid -->
        <div class="stats-card glass-panel">
          <div class="stats-title">我的统计</div>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ myStats.todayPostCount || 0 }}</div>
              <div class="stat-label">今日投稿</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ visitedZonesCount }}</div>
              <div class="stat-label">到访分区</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ totalPosts }}</div>
              <div class="stat-label">累计投稿</div>
            </div>
          </div>
        </div>

        <!-- Emotion bar chart -->
        <div class="section-title">今日情绪分布</div>
        <div class="emotion-bar" v-if="Object.keys(todayEmotions).length">
          <div v-for="(count, emotion) in todayEmotions" :key="emotion" class="emotion-item">
            <span class="emotion-name">{{ emotion }}</span>
            <div class="emotion-track">
              <div class="emotion-fill" :style="{ width: emotionPercent(emotion) + '%' }"></div>
            </div>
            <span class="emotion-count">{{ count }}</span>
          </div>
        </div>
        <div v-else class="empty-hint">今天还没有记录心情</div>

        <!-- Map2D building distribution -->
        <div class="section-title">投稿建筑分布</div>
        <Map2D :buildingWeathers="buildingWeathers" :loading="false" @building-click="onMapBuildingClick" />

        <!-- My posts -->
        <div class="section-title">我的投稿</div>
        <div v-if="myPosts.length">
          <PostCard
            v-for="p in myPosts"
            :key="p.postId"
            :post="p"
            :showDelete="true"
            :canDelete="true"
            :showActions="false"
            @delete="doDeletePost(p.postId)"
            @click="viewBuilding(p.buildingName)"
          />
        </div>
        <div v-else class="empty-hint">还没有投稿，去记录第一条心情吧~</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createLogger } from '../utils/debug.js'
import { api, saveToken, clearToken, saveProfile, clearProfile, getProfile, isLoggedIn } from '../api/index.js'
import Toast from '../components/Toast.vue'

const log = createLogger('Profile')
import { getAvatarUrl, getPickedIcon, savePickedIcon, AVATAR_OPTIONS } from '../utils/avatar.js'
import PostCard from '../components/PostCard.vue'
import Map2D from '../components/Map2D.vue'

const emit = defineEmits(['navigate'])

// Auth
const email = ref('')
const password = ref('')
const loggedIn = ref(false)
const toast = ref(null)

// Profile
const profile = ref({})

// Settings
const showSettings = ref(false)
const editNickname = ref('')
const editAvatar = ref('🌤️')
const editAnon = ref(false)
const avatarOptions = AVATAR_OPTIONS
const saveMsg = ref('')

// Password modal
const showPwdModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const pwdMsg = ref('')
const pwdOk = ref(false)
const pwdSubmitting = ref(false)

// Data
const todayWeather = ref({})
const myPosts = ref([])
const myStats = ref({})
const todayEmotions = ref({})
const visitedZones = ref({})

const visitedZonesCount = computed(() => Object.keys(visitedZones.value).length)
const totalPosts = computed(() => myPosts.value.length)

// Map2D building weathers
const buildingWeathers = computed(() => {
  const map = {}
  for (const post of myPosts.value) {
    if (post.buildingName && post.weatherCode) {
      map[post.buildingName] = post.weatherCode
    }
  }
  return map
})

loggedIn.value = isLoggedIn()
if (loggedIn.value) {
  profile.value = getProfile()
  log.log('初始化', { loggedIn: true, email: profile.value.email, nickname: profile.value.nickname })
  editNickname.value = profile.value.nickname || ''
  editAvatar.value = getPickedIcon()
  editAnon.value = localStorage.getItem('defaultAnon') === 'true'
}

onMounted(() => { if (loggedIn.value) loadData() })

async function loadData() {
  log.log('loadData')
  try {
    const [postsRes, statsRes, weatherRes] = await Promise.all([
      api.getMyPosts(),
      api.getMyStats(),
      api.getTodayWeather()
    ])
    if (postsRes.code === 0) myPosts.value = postsRes.data || []
    if (statsRes.code === 0) {
      myStats.value = statsRes.data
      todayEmotions.value = statsRes.data.todayEmotions || {}
      visitedZones.value = statsRes.data.visitedZones || {}
    }
    if (weatherRes.code === 0) {
      todayWeather.value = weatherRes.data || {}
    }
    log.log('loadData 完成', { posts: myPosts.value.length, hasStats: !!statsRes.data, hasWeather: !!weatherRes.data })
  } catch (e) { log.error('loadData 异常', e) }
}

function mapWeatherFromEmotion(emotion) {
  const map = { '愉悦': 'sunny', '平静': 'cloudy', '焦虑': 'rainy', '低落': 'heavy_rain', '疲惫': 'overcast' }
  return map[emotion] || 'cloudy'
}

// Auth
async function doLogin() {
  if (!email.value.trim() || !password.value.trim()) { toast.value?.show('请输入邮箱和密码', 'warning'); return }
  log.log('doLogin', { email: email.value.trim() })
  const res = await api.login(email.value.trim(), password.value.trim())
  if (res.code === 0) {
    log.log('登录成功')
    saveToken(res.data.token)
    saveProfile(res.data)
    profile.value = getProfile()
    editNickname.value = profile.value.nickname || ''
    loggedIn.value = true
    loadData()
  } else {
    log.log('登录失败', { message: res.message })
    toast.value?.show(res.message || '登录失败', 'error')
  }
}

function doLogout() {
  log.log('doLogout')
  clearToken()
  clearProfile()
  loggedIn.value = false
  myPosts.value = []
  myStats.value = {}
  todayWeather.value = {}
  profile.value = {}
  localStorage.removeItem('pickedAvatar')
  localStorage.removeItem('defaultAnon')
  log.log('已登出')
}

function switchToRegister() {
  log.log('switchToRegister')
  emit('navigate', 'register')
}

// Settings
async function saveAllSettings() {
  log.log('saveAllSettings', { nickname: editNickname.value, avatar: editAvatar.value, anon: editAnon.value })
  try {
    const res = await api.updateProfile({
      nickname: editNickname.value.trim() || undefined,
      avatar: editAvatar.value,
      defaultAnonymous: editAnon.value
    })
    if (res.code === 0) {
      saveProfile({ ...getProfile(), nickname: editNickname.value.trim(), avatar: editAvatar.value, defaultAnonymous: editAnon.value })
      saveMsg.value = '✅ 设置已保存'
    } else {
      saveMsg.value = res.message || '保存失败'
    }
  } catch (e) {
    saveMsg.value = '网络错误'
  }
  setTimeout(() => saveMsg.value = '', 2000)
}

function changePassword() {
  pwdMsg.value = ''
  pwdOk.value = false
  if (!oldPassword.value) { pwdMsg.value = '请输入原密码'; return }
  if (!newPassword.value || newPassword.value.length < 6) { pwdMsg.value = '新密码至少6位'; return }
  if (newPassword.value !== confirmPassword.value) { pwdMsg.value = '两次密码不一致'; return }

  log.log('changePassword 调用API')
  pwdSubmitting.value = true
  api.changePassword(oldPassword.value, newPassword.value).then(res => {
    log.log('changePassword 返回', { code: res.code, message: res.message })
    if (res.code === 0) {
      pwdMsg.value = '✅ 密码修改成功'
      pwdOk.value = true
      setTimeout(() => {
        showPwdModal.value = false
        pwdMsg.value = ''
        oldPassword.value = ''
        newPassword.value = ''
        confirmPassword.value = ''
      }, 1500)
    } else {
      pwdMsg.value = res.message || '修改失败'
    }
  }).catch(() => {
    pwdMsg.value = '网络错误'
    log.log('changePassword 网络错误')
  }).finally(() => {
    pwdSubmitting.value = false
  })
}

// Posts
function doDeletePost(postId) {
  if (!confirm('确定删除？')) return
  log.log('doDeletePost', { postId })
  api.deletePost(postId).then(res => {
    if (res.code === 0) {
      myPosts.value = myPosts.value.filter(p => p.postId !== postId)
      log.log('删除成功')
    } else toast.value?.show(res.message || '删除失败', 'error')
  })
}

function viewBuilding(name) {
  log.log('viewBuilding → navigate(building)', { name })
  emit('navigate', 'building', name)
}

// Map2D
function onMapBuildingClick(data) {
  log.log('onMapBuildingClick', { name: data?.name })
  if (data?.name) viewBuilding(data.name)
}

// Emotion chart
function emotionPercent(emotion) {
  const total = Object.values(todayEmotions.value).reduce((a, b) => a + b, 0)
  return total ? Math.round((todayEmotions.value[emotion] / total) * 100) : 0
}
</script>

<style scoped>
.profile-page {
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a2e 100%);
  overflow-y: auto;
}
.profile-container {
  padding: 24px 16px 80px;
  max-width: 480px;
  margin: 0 auto;
}

/* Auth */
.auth-section { text-align: center; padding-top: 60px; }
.auth-icon { font-size: 48px; margin-bottom: 12px; }
.auth-section h2 { margin-bottom: 8px; font-size: 18px; }
.auth-section p { color: var(--text-secondary); font-size: 13px; margin-bottom: 24px; }
.auth-form {
  display: flex; flex-direction: column; gap: 10px;
  max-width: 280px; margin: 0 auto;
}
.auth-btns { display: flex; gap: 8px; }
.auth-btns .btn { flex: 1; }

/* Profile header */
.profile-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(255,255,255,0.05);
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.06);
  margin-bottom: 16px;
}
.profile-avatar { font-size: 36px; }
.profile-info { flex: 1; }
.profile-nickname { font-size: 14px; font-weight: 600; color: #fff; }
.profile-email { font-size: 11px; color: rgba(255,255,255,0.4); }
.logout-btn {
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255,100,100,0.3);
  background: rgba(255,60,60,0.15);
  color: rgba(255,150,150,0.8);
  font-size: 11px;
  cursor: pointer;
}

/* Settings */
.settings-section { margin-bottom: 16px; }
.settings-title {
  font-size: 12px; color: rgba(255,255,255,0.5);
  cursor: pointer; padding: 4px 0;
  display: flex; align-items: center; gap: 4px;
}
.settings-arrow { font-size: 10px; }
.settings-body {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 10px;
}
.setting-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.setting-row label {
  font-size: 11px; color: rgba(255,255,255,0.5);
  min-width: 50px; flex-shrink: 0;
}
.setting-input {
  width: 130px; font-size: 12px; padding: 4px 8px;
}
.avatar-picker { display: flex; gap: 4px; flex-wrap: wrap; }
.avatar-opt {
  font-size: 18px; cursor: pointer; padding: 2px 4px;
  border-radius: 6px; border: 1px solid transparent;
}
.avatar-opt.picked { border-color: var(--accent); background: rgba(126,200,227,0.2); }
.anon-toggle {
  font-size: 11px; color: rgba(255,255,255,0.6);
  display: flex; align-items: center; gap: 4px; cursor: pointer;
}
.anon-toggle input { width: auto; }
.btn-sm { padding: 4px 10px; border-radius: 6px; border: none; background: var(--accent); color: #fff; font-size: 11px; cursor: pointer; }
.btn-outline { background: transparent; border: 1px solid rgba(255,255,255,0.2); color: rgba(255,255,255,0.7); }
.save-settings-btn { width: 100%; margin-top: 4px; padding: 10px; border-radius: 10px; border: none; background: var(--accent); color: #fff; font-size: 13px; cursor: pointer; }
.save-msg { text-align: center; font-size: 11px; color: #5ac8a0; margin-top: 4px; }

/* Password modal */
.modal-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
}
.modal-card {
  background: #1a1a2e; border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px; padding: 24px; width: 280px;
  display: flex; flex-direction: column; gap: 10px;
}
.modal-title { font-size: 16px; font-weight: 600; color: #fff; text-align: center; margin-bottom: 4px; }
.modal-card input { width: 100%; }
.modal-btns { display: flex; gap: 8px; }
.modal-btns .btn { flex: 1; font-size: 13px; padding: 10px; }
.modal-btns .btn-secondary { background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.12); }
.pwd-msg { text-align: center; font-size: 12px; }
.pwd-msg.ok { color: #5ac8a0; }
.pwd-msg.err { color: #e06060; }

/* Today weather card */
.today-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}
.today-icon { font-size: 44px; }
.today-label { font-size: 11px; color: var(--text-secondary); }
.today-name { font-size: 20px; font-weight: 600; }
.today-summary { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

/* Stats */
.stats-card { margin-bottom: 14px; }
.stats-title { font-size: 13px; color: rgba(255,255,255,0.5); margin-bottom: 10px; }
.stats-grid { display: flex; gap: 12px; }
.stat-item { flex: 1; text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--accent); }
.stat-label { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }

.section-title {
  font-size: 14px; font-weight: 600;
  margin: 16px 0 8px;
}

/* Emotion bar */
.emotion-bar { display: flex; flex-direction: column; gap: 6px; margin-bottom: 4px; }
.emotion-item { display: flex; align-items: center; gap: 8px; }
.emotion-name { width: 40px; font-size: 11px; color: rgba(255,255,255,0.6); text-align: right; }
.emotion-track {
  flex: 1; height: 6px;
  background: rgba(255,255,255,0.06);
  border-radius: 3px; overflow: hidden;
}
.emotion-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 3px;
  transition: width 0.5s;
}
.emotion-count { width: 20px; font-size: 11px; color: rgba(255,255,255,0.4); }

.empty-hint {
  text-align: center;
  padding: 20px 0;
  color: rgba(255,255,255,0.3);
  font-size: 13px;
}
</style>
