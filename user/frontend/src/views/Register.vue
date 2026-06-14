<template>
  <div class="page register-page">
    <div class="register-container">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2>注册</h2>
      <div class="auth-form">
        <input v-model="email" type="email" placeholder="邮箱" />
        <input v-model="password" type="password" placeholder="密码（至少6位）" />
        <input v-model="confirmPassword" type="password" placeholder="确认密码" />
        <button class="btn" :disabled="!canSubmit || submitting" @click="doRegister">
          {{ submitting ? '注册中...' : '注册' }}
        </button>
        <button class="btn btn-secondary" @click="switchToLogin">已有账号？登录</button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { createLogger } from '../utils/debug.js'
import { api, saveToken, saveProfile, isLoggedIn } from '../api/index.js'

const log = createLogger('Register')

const emit = defineEmits(['navigate'])

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const error = ref('')

const canSubmit = computed(() =>
  email.value.trim() &&
  password.value.length >= 6 &&
  password.value === confirmPassword.value
)

function goBack() { log.log('goBack → navigate(my)'); emit('navigate', 'my') }
function switchToLogin() { log.log('switchToLogin → navigate(my)'); emit('navigate', 'my') }

async function doRegister() {
  log.log('doRegister 开始', { email: email.value.trim(), pwdLength: password.value.length })
  if (password.value !== confirmPassword.value) {
    log.log('密码不一致')
    error.value = '两次密码不一致'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    const res = await api.register(email.value.trim(), password.value)
    log.log('API返回', { code: res.code, hasToken: !!res.data?.token })
    if (res.code === 0) {
      saveToken(res.data.token)
      saveProfile(res.data)
      log.log('注册成功，跳转 my')
      emit('navigate', 'my')
    } else {
      error.value = res.message || '注册失败'
      log.log('注册失败', { message: res.message })
    }
  } catch {
    error.value = '网络错误'
    log.log('网络错误')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-page {
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a2e 100%);
  overflow-y: auto;
}
.register-container {
  padding: 24px 16px 80px;
  max-width: 320px;
  margin: 0 auto;
  text-align: center;
}
.register-container h2 { margin-bottom: 20px; }

.back-btn {
  display: block;
  background: none; border: none;
  color: var(--accent); font-size: 14px;
  cursor: pointer; padding: 4px 0;
  margin-bottom: 12px;
  text-align: left;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.auth-form .btn { margin-top: 4px; }

.error-text {
  color: #ff6b6b;
  font-size: 13px;
  margin-top: 12px;
}
</style>
