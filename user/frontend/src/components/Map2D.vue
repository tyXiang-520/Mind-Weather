<template>
  <div ref="containerRef" class="map2d-container">
    <canvas ref="canvasRef" class="map2d-canvas"></canvas>
    <div class="map2d-empty" v-if="!hasAnyPosts && !loading">
      <div class="empty-icon">🗺️</div>
      <div class="empty-title">你的心晴地图待点亮</div>
      <div class="empty-desc">去投稿页写下第一条心情吧~</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { createLogger } from '../utils/debug.js'

const log = createLogger('Map2D')

const props = defineProps({
  /** { buildingName: 'sunny'|'cloudy'|... }  */
  buildingWeathers: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: true }
})

const emit = defineEmits(['buildingClick'])

const containerRef = ref(null)
const canvasRef = ref(null)
const hasAnyPosts = computed(() => Object.keys(props.buildingWeathers).length > 0)

// ═══════════ 建筑 2D 布局 ═══════════
// 手动精炼的鼓楼校区建筑 2D 布局（从 GLB 3D 坐标投影简化）
// 坐标原点对应校园大致中心，y 轴向下（Canvas 坐标系）

const BUILDING_LAYOUT = [
  // ── A区 教学核心 ──
  { name: '教学楼', x: 120, y: 210, w: 42, h: 18, color: '#e8ddd0' },
  { name: '新教学楼', x: 125, y: 240, w: 30, h: 12, color: '#e8ddd0' },
  { name: '图书馆', x: 230, y: 280, w: 55, h: 28, color: '#f0e8d8' },
  { name: '南教学楼', x: 105, y: 260, w: 22, h: 16, color: '#e8ddd0' },
  { name: '蒙民伟楼', x: 170, y: 228, w: 38, h: 28, color: '#e8ddd0' },
  { name: '科技馆', x: 195, y: 205, w: 26, h: 16, color: '#e8ddd0' },
  { name: '校史博物馆', x: 260, y: 310, w: 20, h: 14, color: '#dfd5c0' },
  { name: '小礼堂', x: 295, y: 208, w: 16, h: 12, color: '#e0d5c0' },
  { name: '东南楼', x: 340, y: 235, w: 18, h: 24, color: '#e8ddd0' },
  { name: '南楼', x: 315, y: 260, w: 14, h: 20, color: '#e8ddd0' },

  // ── B区 历史核心 ──
  { name: '北大楼', x: 290, y: 155, w: 40, h: 28, color: '#d8c8a8' },
  { name: '大礼堂', x: 340, y: 178, w: 28, h: 22, color: '#d0c0a0' },
  { name: '西大楼', x: 255, y: 168, w: 22, h: 26, color: '#e8ddd0' },
  { name: '东大楼', x: 330, y: 148, w: 22, h: 26, color: '#e8ddd0' },
  { name: '辛壬楼', x: 270, y: 128, w: 16, h: 18, color: '#e0d0b8' },
  { name: '戊己庚楼', x: 246, y: 130, w: 18, h: 14, color: '#e0d0b8' },
  { name: '丙丁楼', x: 252, y: 110, w: 16, h: 14, color: '#e0d0b8' },
  { name: '甲乙楼', x: 278, y: 110, w: 16, h: 14, color: '#e0d0b8' },
  { name: '东北楼', x: 345, y: 128, w: 16, h: 18, color: '#e8ddd0' },
  { name: '信息管理服务中心', x: 298, y: 115, w: 20, h: 12, color: '#e8ddd0' },

  // ── C区 文科楼群 ──
  { name: '逸夫馆Ⅰ区', x: 80, y: 220, w: 24, h: 38, color: '#e8ddd0' },
  { name: '逸夫馆Ⅱ区1', x: 60, y: 225, w: 18, h: 28, color: '#e8ddd0' },
  { name: '逸夫馆Ⅱ区2', x: 60, y: 255, w: 18, h: 28, color: '#e8ddd0' },
  { name: '逸夫馆Ⅲ区1', x: 40, y: 228, w: 16, h: 20, color: '#e8ddd0' },
  { name: '逸夫馆Ⅲ区2', x: 40, y: 248, w: 16, h: 20, color: '#e8ddd0' },
  { name: '逸夫馆Ⅲ区3', x: 40, y: 268, w: 16, h: 20, color: '#e8ddd0' },
  { name: '费彝民楼A栋', x: 105, y: 240, w: 22, h: 16, color: '#e8ddd0' },
  { name: '费彝民楼B栋', x: 105, y: 258, w: 22, h: 14, color: '#e8ddd0' },
  { name: '田家炳艺术学院', x: 85, y: 300, w: 28, h: 16, color: '#e8ddd0' },
  { name: '逸夫管理科学楼', x: 140, y: 300, w: 36, h: 20, color: '#e8ddd0' },
  { name: '南大出版社', x: 68, y: 285, w: 16, h: 12, color: '#e8ddd0' },
  { name: '建良楼', x: 60, y: 310, w: 18, h: 12, color: '#e8ddd0' },

  // ── D区 运动场馆 ──
  { name: '苏浙运动场', x: 420, y: 68, w: 50, h: 30, color: '#5a8a4a' },
  { name: '吕志和游泳馆', x: 420, y: 110, w: 50, h: 40, color: '#d8c8b0' },

  // ── E区 理科/生活 ──
  { name: '西南楼', x: 380, y: 260, w: 16, h: 24, color: '#e8ddd0' },
  { name: '知行楼', x: 360, y: 300, w: 20, h: 16, color: '#e8ddd0' },
  { name: '树华楼', x: 175, y: 270, w: 18, h: 12, color: '#e8ddd0' },
  { name: '声学楼', x: 200, y: 270, w: 22, h: 14, color: '#e8ddd0' },
  { name: '声学西楼', x: 180, y: 272, w: 16, h: 10, color: '#e8ddd0' },
  { name: '物理楼', x: 130, y: 195, w: 28, h: 22, color: '#e8ddd0' },
  { name: '健忠楼', x: 155, y: 290, w: 14, h: 10, color: '#e8ddd0' },
  { name: '低温实验楼', x: 145, y: 185, w: 16, h: 14, color: '#e8ddd0' },
  { name: '李四光旧居', x: 100, y: 160, w: 14, h: 10, color: '#d8c0a0' },
  { name: '罗根泽旧居', x: 110, y: 155, w: 14, h: 10, color: '#d8c0a0' },
  { name: '赛珍珠故居', x: 120, y: 150, w: 16, h: 12, color: '#d8c0a0' },
  { name: '创新中心', x: 135, y: 210, w: 16, h: 14, color: '#e8ddd0' },
  { name: '斗鸡闸', x: 155, y: 158, w: 14, h: 12, color: '#d8c0a0' },
  { name: '水电管理中心', x: 165, y: 155, w: 16, h: 10, color: '#e8ddd0' },

  // ── F区 餐饮生活 ──
  { name: '南园餐厅', x: 390, y: 400, w: 30, h: 20, color: '#ece0d0' },
  { name: '教工食堂', x: 370, y: 415, w: 16, h: 12, color: '#ece0d0' },
  { name: '教育超市', x: 400, y: 425, w: 18, h: 12, color: '#ece0d0' },
  { name: '南园综合楼', x: 420, y: 410, w: 22, h: 18, color: '#ece0d0' },
  { name: '南大浴室', x: 430, y: 435, w: 14, h: 10, color: '#d4b896' },

  // ── G区 南园宿舍A ──
  { name: '南园1舍', x: 70, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园2舍', x: 88, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园3舍', x: 106, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园4舍', x: 124, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园5舍', x: 142, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园6舍', x: 160, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园7舍', x: 178, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园17舍', x: 230, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园18舍', x: 248, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '南园19舍', x: 266, y: 420, w: 14, h: 24, color: '#d4b896' },
  { name: '东苑宿舍', x: 30, y: 415, w: 20, h: 28, color: '#d4b896' },
  { name: '校医院', x: 310, y: 410, w: 18, h: 16, color: '#d0c0b0' },
  { name: '松林楼', x: 290, y: 430, w: 16, h: 14, color: '#d4b896' },
  { name: '中山楼', x: 340, y: 430, w: 16, h: 14, color: '#d4b896' },

  // ── H区 陶园/南园B ──
  { name: '综合服务大厅', x: 370, y: 460, w: 22, h: 14, color: '#ece0d0' },
  { name: '陶园1舍', x: 60, y: 460, w: 14, h: 24, color: '#d4b896' },
  { name: '陶园2舍', x: 78, y: 460, w: 14, h: 24, color: '#d4b896' },
  { name: '陶园3舍', x: 96, y: 460, w: 14, h: 24, color: '#d4b896' },
  { name: '陶园南楼', x: 114, y: 460, w: 16, h: 20, color: '#d4b896' },
  { name: '南园15舍', x: 196, y: 460, w: 14, h: 24, color: '#d4b896' },
  { name: '南园16舍', x: 214, y: 460, w: 14, h: 24, color: '#d4b896' },

  // ── I区 南园宿舍C ──
  { name: '菜鸟驿站', x: 380, y: 480, w: 16, h: 10, color: '#ece0d0' },
  { name: '南园13舍', x: 140, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '南园14舍', x: 158, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '南园20舍', x: 285, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '南园21舍', x: 303, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '有园宾馆', x: 330, y: 490, w: 20, h: 14, color: '#d8c0a0' },
  { name: '南园教学楼', x: 320, y: 470, w: 22, h: 16, color: '#e8ddd0' },
  { name: '荟萃楼', x: 350, y: 470, w: 16, h: 12, color: '#e8ddd0' },
  { name: '后勤服务集团', x: 400, y: 495, w: 18, h: 12, color: '#ece0d0' },
  { name: '校园110报警中心', x: 410, y: 510, w: 14, h: 10, color: '#ece0d0' },
  { name: '校园纪念品商店', x: 395, y: 510, w: 14, h: 10, color: '#ece0d0' },

  // ── J区 南园宿舍D ──
  { name: '南园8舍', x: 200, y: 460, w: 14, h: 24, color: '#d4b896' },
  { name: '南园11舍', x: 64, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '南园12舍', x: 82, y: 500, w: 14, h: 24, color: '#d4b896' },
  { name: '外教公寓', x: 360, y: 520, w: 16, h: 12, color: '#d8c0a0' },
  { name: '拉贝故居', x: 430, y: 460, w: 16, h: 14, color: '#d8c0a0' },
  { name: '南苑宾馆一号楼', x: 440, y: 500, w: 18, h: 14, color: '#d8c0a0' },
  { name: '南苑宾馆二号楼', x: 440, y: 516, w: 18, h: 14, color: '#d8c0a0' },

  // ── K区 北区科研 ──
  { name: '工程管理学院', x: 400, y: 140, w: 30, h: 20, color: '#e8ddd0' },
  { name: '天文楼', x: 380, y: 95, w: 22, h: 18, color: '#e8ddd0' },
  { name: '协鑫楼', x: 410, y: 170, w: 22, h: 16, color: '#e8ddd0' },
  { name: '平仓楼', x: 460, y: 155, w: 16, h: 22, color: '#e8ddd0' },
  { name: '平仓楼北楼', x: 460, y: 130, w: 16, h: 18, color: '#e8ddd0' },
  { name: '华龙楼1号', x: 440, y: 110, w: 14, h: 16, color: '#e8ddd0' },
  { name: '华龙楼2号', x: 440, y: 88, w: 14, h: 16, color: '#e8ddd0' },
  { name: '华龙楼3号', x: 440, y: 66, w: 14, h: 16, color: '#e8ddd0' },

  // ── L区 综合/其他 ──
  { name: '唐仲英楼', x: 430, y: 195, w: 28, h: 20, color: '#e8ddd0' },
  { name: '安中楼', x: 440, y: 42, w: 24, h: 20, color: '#e8ddd0' },
  { name: '曾宪梓楼', x: 400, y: 55, w: 22, h: 18, color: '#e8ddd0' },
  { name: '实验楼', x: 380, y: 120, w: 18, h: 16, color: '#e8ddd0' },
  { name: '科学楼', x: 350, y: 100, w: 20, h: 18, color: '#e8ddd0' },
  { name: '中美文化研究中心', x: 420, y: 210, w: 22, h: 18, color: '#e8ddd0' },
  { name: '西苑宾馆', x: 460, y: 220, w: 16, h: 14, color: '#d8c0a0' },
]

// ═══════════ 天气色映射 ═══════════
const WEATHER_COLORS = {
  sunny: '#f0c040',
  cloudy: '#c0c8d0',
  overcast: '#888890',
  rainy: '#6699bb',
  heavy_rain: '#5577aa',
  thunderstorm: '#665588'
}

const WEATHER_EMOJI = {
  sunny: '☀️', cloudy: '⛅', overcast: '☁️',
  rainy: '🌧️', heavy_rain: '⛈️', thunderstorm: '🌩️'
}

// ═══════════ Canvas 渲染 ═══════════
let animationId, hoveredBuilding = null
let isAnimating = false

const MAP_W = 520  // Canvas 内部坐标宽
const MAP_H = 560  // Canvas 内部坐标高

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  log.log('draw', { buildingCount: Object.keys(props.buildingWeathers).length, hovered: hoveredBuilding })
  const ctx = canvas.getContext('2d')
  const dpr = Math.min(window.devicePixelRatio, 2)
  const rect = canvas.parentElement.getBoundingClientRect()
  const w = rect.width
  const h = rect.height || w * (MAP_H / MAP_W)

  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const sx = w / MAP_W
  const sy = h / MAP_H
  const scale = Math.min(sx, sy)
  const ox = (w - MAP_W * scale) / 2
  const oy = (h - MAP_H * scale) / 2

  // 背景
  ctx.fillStyle = '#0f0f1e'
  ctx.fillRect(0, 0, w, h)

  ctx.save()
  ctx.translate(ox, oy)
  ctx.scale(scale, scale)

  // 绘制建筑
  for (const b of BUILDING_LAYOUT) {
    const weather = props.buildingWeathers[b.name]
    if (!weather) continue  // 未投稿的建筑不显示

    const isHovered = hoveredBuilding === b.name
    const weatherColor = WEATHER_COLORS[weather] || '#888'

    // 建筑阴影
    ctx.fillStyle = 'rgba(0,0,0,0.3)'
    ctx.fillRect(b.x + 2, b.y + 2, b.w, b.h)

    // 建筑主体
    const grad = ctx.createLinearGradient(b.x, b.y, b.x, b.y + b.h)
    grad.addColorStop(0, weatherColor)
    grad.addColorStop(0.4, weatherColor + 'cc')
    grad.addColorStop(1, b.color + '88')
    ctx.fillStyle = grad
    ctx.fillRect(b.x, b.y, b.w, b.h)

    // 边框
    ctx.strokeStyle = isHovered ? '#fff' : weatherColor + '66'
    ctx.lineWidth = isHovered ? 1.5 : 0.5
    ctx.strokeRect(b.x, b.y, b.w, b.h)

    // 发光
    if (isHovered) {
      ctx.shadowColor = weatherColor
      ctx.shadowBlur = 8
      ctx.fillStyle = weatherColor + '22'
      ctx.fillRect(b.x, b.y, b.w, b.h)
      ctx.shadowBlur = 0
    }

    // 标签
    if (b.w > 20 && b.h > 12) {
      ctx.fillStyle = 'rgba(255,255,255,0.85)'
      ctx.font = `${Math.max(7, b.h * 0.3)}px sans-serif`
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(b.name, b.x + b.w / 2, b.y + b.h / 2)
    }
  }

  // 区域标签
  ctx.fillStyle = 'rgba(255,255,255,0.15)'
  ctx.font = '9px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('南京大学鼓楼校区', MAP_W / 2, 15)

  ctx.restore()
}

// ═══════════ 交互 ═══════════
function getBuildingAt(clientX, clientY) {
  const canvas = canvasRef.value
  if (!canvas) return null
  const rect = canvas.getBoundingClientRect()
  const w = rect.width
  const h = rect.height
  const sx = w / MAP_W
  const sy = h / MAP_H
  const scale = Math.min(sx, sy)
  const ox = (w - MAP_W * scale) / 2
  const oy = (h - MAP_H * scale) / 2

  const mx = (clientX - rect.left - ox) / scale
  const my = (clientY - rect.top - oy) / scale

  // 从上层向下遍历（后绘制的在视觉上层，但这里倒序遍历优先匹配小建筑）
  for (const b of [...BUILDING_LAYOUT].reverse()) {
    const weather = props.buildingWeathers[b.name]
    if (!weather) continue
    if (mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h) {
      return b
    }
  }
  return null
}

function onMouseMove(e) {
  const b = getBuildingAt(e.clientX, e.clientY)
  if (b?.name !== hoveredBuilding) {
    hoveredBuilding = b ? b.name : null
    log.log('hover', { building: hoveredBuilding })
  }
}

function onClick(e) {
  const b = getBuildingAt(e.clientX, e.clientY)
  if (b) {
    log.log('建筑点击', { name: b.name, weather: props.buildingWeathers[b.name] })
    emit('buildingClick', { name: b.name, weather: props.buildingWeathers[b.name] })
  } else {
    log.log('点击空白区域')
  }
}

function onResize() {
  draw()
}

function startAnimation() {
  if (isAnimating) return
  isAnimating = true
  animationId = requestAnimationFrame(function loop() {
    // v-show 隐藏时跳过渲染
    if (containerRef.value && containerRef.value.clientWidth > 0) {
      draw()
    }
    animationId = requestAnimationFrame(loop)
  })
}

function stopAnimation() {
  isAnimating = false
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

onMounted(() => {
  draw()
  startAnimation()
  window.addEventListener('resize', onResize)
  const canvas = canvasRef.value
  if (canvas) {
    canvas.addEventListener('mousemove', onMouseMove)
    canvas.addEventListener('click', onClick)
  }
})

onBeforeUnmount(() => {
  stopAnimation()
  window.removeEventListener('resize', onResize)
  const canvas = canvasRef.value
  if (canvas) {
    canvas.removeEventListener('mousemove', onMouseMove)
    canvas.removeEventListener('click', onClick)
  }
})

watch(() => props.buildingWeathers, () => { draw() }, { deep: true })

defineExpose({})
</script>

<style scoped>
.map2d-container {
  width: 100%;
  aspect-ratio: 520 / 560;
  max-height: 60vh;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #0f0f1e;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.map2d-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.map2d-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.empty-icon { font-size: 40px; margin-bottom: 12px; opacity: 0.6; }
.empty-title { font-size: 16px; font-weight: 600; color: rgba(255,255,255,0.5); margin-bottom: 4px; }
.empty-desc { font-size: 12px; color: rgba(255,255,255,0.25); }
</style>
