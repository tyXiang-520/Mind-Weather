<template>
  <div class="page my-page">
    <div class="my-container">
      <!-- Not logged in -->
      <div v-if="!loggedIn" class="empty-state">
        <div class="empty-icon">🔐</div>
        <h2>需要登录</h2>
        <p>登录后才能查看个人气象空间</p>
        <div class="login-form">
          <input v-model="email" type="email" placeholder="邮箱" />
          <input v-model="password" type="password" placeholder="密码" />
          <div class="login-btns">
            <button class="btn" @click="doLogin">登录</button>
            <button class="btn btn-secondary" @click="doRegister">注册</button>
          </div>
        </div>
      </div>

      <!-- No posts yet -->
      <div v-else-if="myPosts.length === 0" class="empty-state">
        <div class="empty-icon">🗺️</div>
        <h2>你的心晴地图待点亮</h2>
        <p>去投稿页写下第一条心情吧~</p>
      </div>

      <!-- Has posts -->
      <template v-else>
        <h2 class="page-title">我的天气</h2>

        <!-- Today's weather card -->
        <div class="today-card glass-panel">
          <div class="today-icon">{{ todayWeather.icon || '⛅' }}</div>
          <div class="today-info">
            <div class="today-label">今日天气</div>
            <div class="today-name">{{ todayWeather.name || '多云' }}</div>
            <div class="today-summary">{{ todayWeather.summary || '今天还没有记录心情' }}</div>
          </div>
        </div>

        <!-- 2D 校园地图 -->
        <div class="section-title">投稿建筑分布</div>
        <Map2D
          :buildingWeathers="buildingWeathers"
          :loading="false"
          @building-click="onMapBuildingClick"
        />

        <!-- Post history -->
        <div class="section-title">投稿历史</div>
        <div v-for="post in myPosts" :key="post.postId" class="post-item glass-panel">
          <div class="post-weather">{{ post.weatherIcon }} {{ post.weatherName }}</div>
          <div class="post-content">{{ post.content }}</div>
          <div class="post-meta">{{ post.areaName }} · {{ post.createdAt }}</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api, saveToken, isLoggedIn } from '../api/index.js'
import Map2D from '../components/Map2D.vue'

const email = ref('')
const password = ref('')
const loggedIn = ref(false)
const todayWeather = ref({})
const myPosts = ref([])

// 从投稿中提取每栋建筑的天气
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

async function doLogin() {
  if (!email.value.trim() || !password.value.trim()) {
    alert('请输入邮箱和密码'); return
  }
  const res = await api.login(email.value, password.value)
  if (res.code === 0) { saveToken(res.data.token); loggedIn.value = true; loadData() }
  else alert(res.message)
}

async function doRegister() {
  if (!email.value.trim() || !password.value.trim()) {
    alert('请输入邮箱和密码'); return
  }
  if (password.value.length < 6) {
    alert('密码至少6位'); return
  }
  const res = await api.register(email.value, password.value)
  if (res.code === 0) { saveToken(res.data.token); loggedIn.value = true; loadData() }
  else alert(res.message)
}

async function loadData() {
  try {
    const [weather, posts] = await Promise.all([
      api.getTodayWeather(),
      api.getMyPosts()
    ])
    if (weather.code === 0) todayWeather.value = weather.data
    if (posts.code === 0) {
      // 注入 mock 建筑名和天气（后端完善后由 API 直接返回）
      myPosts.value = posts.data.map(p => ({
        ...p,
        buildingName: p.areaName || p.buildingName,
        weatherCode: p.weatherCode || mapWeatherFromEmotion(p.emotionType)
      }))
    }
  } catch (e) {
    // API 不可用时用 mock 数据
    console.log('API 未连接，使用 mock 个人数据')
    myPosts.value = [
      { postId: 1, weatherIcon: '☀️', weatherName: '晴', weatherCode: 'sunny', buildingName: '北大楼', content: '今天在草坪上晒太阳', areaName: 'B区 历史核心', createdAt: '2 小时前' },
      { postId: 2, weatherIcon: '🌧️', weatherName: '小雨', weatherCode: 'rainy', buildingName: '图书馆', content: 'ddl压力好大...', areaName: 'A区 教学核心', createdAt: '5 小时前' },
      { postId: 3, weatherIcon: '⛅', weatherName: '多云', weatherCode: 'cloudy', buildingName: '南园餐厅', content: '食堂的麻辣香锅真不错', areaName: 'F区 餐饮生活', createdAt: '昨天' },
    ]
  }
}

function mapWeatherFromEmotion(emotion) {
  const map = { '愉悦': 'sunny', '平静': 'cloudy', '焦虑': 'rainy', '低落': 'heavy_rain', '疲惫': 'overcast' }
  return map[emotion] || 'cloudy'
}

function onMapBuildingClick(data) {
  console.log('2D地图点击建筑:', data)
}

onMounted(() => { if (loggedIn.value) loadData() })
</script>

<style scoped>
.my-page { background: linear-gradient(180deg, #0a0a1a 0%, #1a1a2e 100%); overflow-y: auto; }
.my-container { padding: 24px 16px 20px; max-width: 480px; margin: 0 auto; }

.empty-state {
  text-align: center;
  padding-top: 80px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-state h2 { font-size: 18px; margin-bottom: 8px; }
.empty-state p { color: var(--text-secondary); font-size: 13px; margin-bottom: 24px; }

.login-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 280px;
  margin: 0 auto;
}
.login-btns { display: flex; gap: 8px; }
.login-btns .btn { flex: 1; }

.page-title { font-size: 20px; font-weight: 700; margin-bottom: 16px; }

.today-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.today-icon { font-size: 44px; }
.today-label { font-size: 11px; color: var(--text-secondary); }
.today-name { font-size: 20px; font-weight: 600; }
.today-summary { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 20px 0 10px;
}

.area-bars { display: flex; flex-direction: column; gap: 8px; }
.area-bar { display: flex; align-items: center; gap: 8px; }
.bar-label { width: 48px; font-size: 11px; color: var(--text-secondary); text-align: right; flex-shrink: 0; }
.bar-track { flex: 1; height: 8px; background: rgba(255,255,255,0.06); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--accent); border-radius: 4px; transition: width 0.5s; min-width: 4px; }
.bar-fill.empty { background: rgba(255,255,255,0.08); }
.bar-count { width: 20px; font-size: 12px; color: var(--text-secondary); }

.post-item { margin-bottom: 8px; }
.post-weather { font-size: 16px; margin-bottom: 4px; }
.post-content { font-size: 13px; margin-bottom: 4px; }
.post-meta { font-size: 11px; color: var(--text-secondary); }
</style>
