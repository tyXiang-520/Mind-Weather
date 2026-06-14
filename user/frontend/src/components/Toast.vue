<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="visible" class="toast-container" :class="type">
        <span class="toast-icon">{{ icon }}</span>
        <span class="toast-text">{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('info')

const icons = { success: '✓', error: '✕', info: 'ℹ', warning: '⚠' }
const icon = computed(() => icons[type.value] || icons.info)

let timer = null

function show(msg, t = 'info', duration = 2500) {
  message.value = msg
  type.value = t
  visible.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, duration)
}

defineExpose({ show })
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  pointer-events: none;
}

.toast-container.success {
  background: rgba(90, 200, 160, 0.85);
  color: #fff;
}

.toast-container.error {
  background: rgba(220, 80, 80, 0.85);
  color: #fff;
}

.toast-container.info {
  background: rgba(126, 200, 227, 0.85);
  color: #fff;
}

.toast-container.warning {
  background: rgba(240, 168, 96, 0.85);
  color: #fff;
}

.toast-icon {
  font-size: 16px;
  font-weight: 700;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.8);
}
</style>
