<template>
  <div ref="containerRef" class="nju-scene">
    <!-- 加载进度 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-card">
        <div class="loading-spinner"></div>
        <div class="loading-text">加载校园模型中...</div>
        <div class="loading-bar-track">
          <div class="loading-bar-fill" :style="{ width: loadProgress + '%' }"></div>
        </div>
        <div class="loading-pct">{{ loadProgress }}%</div>
      </div>
    </div>

    <!-- 建筑标签层 -->
    <div class="labels-layer">
      <div
        v-for="label in visibleLabels"
        :key="label.name"
        class="building-label"
        :style="label.style"
        @click="onLabelClick(label)"
      >
        <span class="label-dot"></span>
        <span class="label-text">{{ label.displayName }}</span>
      </div>
    </div>

    <!-- 分区标签层 -->
    <div class="zone-labels-layer">
      <div
        v-for="zlabel in visibleZoneLabels"
        :key="zlabel.id"
        class="zone-label"
        :style="zlabel.style"
      >
        <span class="zone-label-icon">{{ getWeatherIcon(zlabel.weather) }}</span>
        <span class="zone-label-text">{{ zlabel.id }}区 {{ zlabel.name }}</span>
      </div>
    </div>

    <!-- 分区天气调试面板（仅 debug 模式显示） -->
    <div v-if="debug" class="zone-weather-panel">
      <div class="zone-panel-title">分区天气调试</div>
      <div class="zone-grid">
        <div
          v-for="zone in zoneList"
          :key="zone.id"
          class="zone-row"
        >
          <span class="zone-label">{{ zone.id }}区</span>
          <select
            class="zone-select"
            :value="zone.weather"
            @change="setZoneWeatherHandler(zone.id, $event.target.value)"
          >
            <option v-for="w in weatherTypes" :key="w.code" :value="w.code">
              {{ w.icon }} {{ w.label }}
            </option>
          </select>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { createLogger } from '../utils/debug.js'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js'
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js'
import { extractBuildings, resolveBuildingName } from '../three/BuildingRegistry.js'
import { WeatherSystem } from '../three/WeatherSystem.js'
import { getZoneByBuilding, getAllZones, getZoneWeather, setZoneWeather } from '../three/ZoneData.js'

const log = createLogger('NJU3D')

const props = defineProps({
  weather: { type: String, default: 'cloudy' },
  debug: { type: Boolean, default: false }
})

const emit = defineEmits(['buildingClick', 'ready', 'zoneWeatherChange'])

// ═══════════ 天气色调映射 ═══════════
const WEATHER_COLORS = {
  sunny: 0xf0c040,
  cloudy: 0xcccccc,
  overcast: 0x888888,
  rainy: 0x5588aa,
  heavy_rain: 0x3366aa,
  thunderstorm: 0x6644aa,
  snow: 0xe8eeff
}

// ═══════════ 状态 ═══════════
const containerRef = ref(null)
const loading = ref(true)
const loadProgress = ref(0)
const currentWeather = ref(props.weather)
const visibleLabels = ref([])
const visibleZoneLabels = ref([])

const weatherTypes = [
  { code: 'sunny', icon: '☀️', label: '晴' },
  { code: 'cloudy', icon: '⛅', label: '多云' },
  { code: 'overcast', icon: '☁️', label: '阴' },
  { code: 'rainy', icon: '🌧️', label: '雨' },
  { code: 'heavy_rain', icon: '⛈️', label: '暴雨' },
  { code: 'thunderstorm', icon: '🌩️', label: '雷暴' },
  { code: 'snow', icon: '❄️', label: '雪' }
]

// 分区列表（响应式，用于调试面板）
const zoneList = ref(getAllZones().map(z => ({ id: z.id, name: z.name, weather: z.weather })))

// ═══════════ Three.js 对象 ═══════════
let scene, camera, renderer, controls, composer, bloomPass
let weatherSystem, animationId
let gltfScene, buildingMap
let zoneIndicators = []  // 分区天气指示器
let zonePlanes = []       // 分区地面色块

// ═══════════ 初始化 ═══════════
onMounted(() => {
  log.log('onMounted 初始化场景')
  initScene()
  animate()
})

onBeforeUnmount(() => {
  log.log('onBeforeUnmount 销毁场景')
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  if (controls) controls.dispose()
  if (weatherSystem) weatherSystem.dispose()
  if (scene) {
    scene.traverse(child => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach(m => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
  }
  if (composer) composer.dispose()
  if (renderer) {
    renderer.dispose()
    renderer.domElement?.remove()
  }
})

// ═══════════ 场景初始化 ═══════════
function initScene() {
  const container = containerRef.value
  if (!container) { log.log('initScene 跳过 — container 为空'); return }
  log.log('initScene', { w: container.clientWidth, h: container.clientHeight })

  const w = container.clientWidth
  const h = container.clientHeight

  // ── 场景 ──
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x87CEEB) // 天蓝色背景
  scene.fog = new THREE.FogExp2(0xc8d0d8, 0.000025)

  // ── 相机 ──
  camera = new THREE.PerspectiveCamera(45, w / h, 1, 800)
  camera.position.set(0, 120, 60)
  camera.lookAt(0, 0, -25)

  // ── 渲染器 ──
  renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: false,
    powerPreference: 'high-performance'
  })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 0.9
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  container.appendChild(renderer.domElement)

  // ── 后处理 ──
  composer = new EffectComposer(renderer)
  composer.addPass(new RenderPass(scene, camera))
  bloomPass = new UnrealBloomPass(
    new THREE.Vector2(w / 2, h / 2),  // 半分辨率 bloom，大幅降负载
    0.2,   // strength
    0.4,   // radius
    0.5    // threshold
  )
  composer.addPass(bloomPass)

  // ── 光照 ──
  setupLighting()

  // ── OrbitControls ──
  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(0, 0, -25)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 15
  controls.maxDistance = 250
  controls.maxPolarAngle = Math.PI / 2.2
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.15
  controls.update()

  // ── 加载模型 ──
  loadModel()

  // ── 天气系统（按分区独立创建，模型加载后由 assignBuildingsToZones 初始化）──
  weatherSystem = new WeatherSystem(scene)
  weatherSystem.bloomPass = bloomPass

  // ── 事件 ──
  window.addEventListener('resize', onResize)
  renderer.domElement.addEventListener('click', onCanvasClick)

  // ── 动画 ──
  animate()
}

// ═══════════ 光照 ═══════════
function setupLighting() {
  // 环境光 —— 降低避免过曝
  const ambient = new THREE.AmbientLight(0x8899aa, 0.5)
  scene.add(ambient)

  // 主方向光（太阳）
  const sun = new THREE.DirectionalLight(0xfff8e8, 3.0)
  sun.position.set(60, 80, -40)
  sun.castShadow = true
  sun.shadow.mapSize.width = 2048
  sun.shadow.mapSize.height = 2048
  sun.shadow.camera.left = -120
  sun.shadow.camera.right = 120
  sun.shadow.camera.top = 120
  sun.shadow.camera.bottom = -120
  sun.shadow.camera.near = 1
  sun.shadow.camera.far = 300
  sun.shadow.bias = -0.0005
  scene.add(sun)

  // 补光（天空散射）
  const fill = new THREE.DirectionalLight(0x8899cc, 0.4)
  fill.position.set(-30, 30, 40)
  scene.add(fill)

  // 半球光
  const hemi = new THREE.HemisphereLight(0x8899cc, 0x445533, 0.3)
  scene.add(hemi)
}

// ═══════════ 加载 GLB 模型 ═══════════
function loadModel() {
  const loader = new GLTFLoader()

  // Draco 解码器
  const dracoLoader = new DRACOLoader()
  dracoLoader.setDecoderPath('/draco/')
  dracoLoader.setDecoderConfig({ type: 'js' })
  loader.setDRACOLoader(dracoLoader)

  loader.load(
    '/models/NJUmap.glb',
    (gltf) => {
      console.log('✅ NJUmap.glb 加载成功')
      gltfScene = gltf.scene

      // 把模型放到场景中
      scene.add(gltfScene)

      // 提取建筑
      buildingMap = extractBuildings(gltfScene)
      console.log(`🏠 识别到 ${buildingMap.size} 栋建筑`)

      // 分配建筑到分区 + 创建分区天气指示器
      assignBuildingsToZones()
      createZoneIndicators()
      initZoneLabels()

      // 生成标签
      updateLabels()

      // 隐藏加载界面
      loading.value = false
      emit('ready')
    },
    (progress) => {
      if (progress.total > 0) {
        loadProgress.value = Math.round((progress.loaded / progress.total) * 100)
      }
    },
    (error) => {
      log.error('❌ 模型加载失败:', error)
      loading.value = false
    }
  )
}

// ═══════════ 标签系统 ═══════════
function updateLabels() {
  if (!buildingMap) return

  const labels = []
  for (const building of buildingMap.values()) {
    if (!building.center) continue

    labels.push({
      name: building.name,
      displayName: building.displayName,
      worldPos: building.center.clone(),
      screenPos: new THREE.Vector3(),
      style: {}
    })
  }

  visibleLabels.value = labels
}

function updateLabelPositions() {
  if (!camera || !containerRef.value) return

  const cw = containerRef.value.clientWidth
  const ch = containerRef.value.clientHeight

  for (const label of visibleLabels.value) {
    const pos = label.worldPos.clone()
    pos.y += 2.5 // 标签放在建筑上方
    pos.project(camera)

    const rawX = (pos.x * 0.5 + 0.5) * cw
    const rawY = (-pos.y * 0.5 + 0.5) * ch

    // 在相机后面或屏幕外隐藏
    if (pos.z > 1 || rawX < -50 || rawX > cw + 50 || rawY < -50 || rawY > ch + 50) {
      label.style = { display: 'none' }
    } else {
      const dist = Math.round(camera.position.distanceTo(label.worldPos))
      const opacity = Math.max(0, Math.min(1, 1 - (dist - 80) / 120))
      const scale = Math.max(0.5, Math.min(1, 1 - (dist - 60) / 140))

      const newX = Math.round(rawX)
      const newY = Math.round(rawY)
      const lastX = label._x || 0; const lastY = label._y || 0
      // 变化 ≤ 2px 不更新，避免亚像素抖动
      if (Math.abs(newX - lastX) <= 2 && Math.abs(newY - lastY) <= 2) continue
      label._x = newX; label._y = newY

      label.style = {
        display: 'flex',
        left: newX + 'px',
        top: newY + 'px',
        opacity,
        transform: `translate(-50%, -50%) scale(${scale})`
      }
    }
  }
}

// ═══════════ 交互 ═══════════
function onCanvasClick(event) {
  if (!camera || !gltfScene || !buildingMap) {
    log.log('onCanvasClick 跳过 — 场景未就绪')
    return
  }

  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)

  const meshes = []
  gltfScene.traverse((child) => {
    if (child.isMesh) meshes.push(child)
  })

  const intersects = raycaster.intersectObjects(meshes, false)
  if (intersects.length > 0) {
    const hitName = resolveHitBuilding(intersects[0].object)
    log.log('3D点击', { hitName, intersectCount: intersects.length })
    if (hitName && buildingMap.has(hitName)) {
      const building = buildingMap.get(hitName)
      const zone = building.zone
      log.log('命中建筑', { name: building.displayName, zone: zone?.id })
      emit('buildingClick', {
        name: building.name,
        displayName: building.displayName,
        center: building.center,
        zone: zone ? { id: zone.id, name: zone.name, weather: zone.weather } : null
      })
    }
  } else {
    log.log('3D点击空白区域')
  }
}

function resolveHitBuilding(mesh) {
  // 向上遍历找到有意义的节点名
  let obj = mesh
  while (obj) {
    const name = obj.name || obj.userData?.name
    if (name) {
      const resolved = resolveBuildingName(name)
      if (resolved) return resolved
    }
    obj = obj.parent
  }

  // Fallback: 直接用 mesh 名或父节点名
  const meshName = mesh.name || mesh.parent?.name || ''
  return resolveBuildingName(meshName)
}

function onLabelClick(label) {
  log.log('标签点击', { name: label.displayName })
  const zone = getZoneByBuilding(label.name)
  emit('buildingClick', {
    name: label.name,
    displayName: label.displayName,
    center: label.worldPos,
    zone: zone ? { id: zone.id, name: zone.name, weather: zone.weather } : null
  })
}

// ═══════════ 分区天气系统 ═══════════

// 为每个建筑标记所属分区 + 计算分区边界
function assignBuildingsToZones() {
  if (!buildingMap) return

  // 先分配
  for (const [bName, building] of buildingMap) {
    const zone = getZoneByBuilding(bName)
    building.zone = zone
  }

  // 计算每个分区的边界和天气发射器
  const zoneBounds = new Map() // zoneId → { cx, cz, minX, maxX, minZ, maxZ }
  for (const building of buildingMap.values()) {
    if (!building.zone || !building.center) continue
    const zid = building.zone.id
    if (!zoneBounds.has(zid)) {
      zoneBounds.set(zid, { cx: 0, cz: 0, minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity, count: 0 })
    }
    const b = zoneBounds.get(zid)
    b.cx += building.center.x
    b.cz += building.center.z
    b.minX = Math.min(b.minX, building.center.x)
    b.maxX = Math.max(b.maxX, building.center.x)
    b.minZ = Math.min(b.minZ, building.center.z)
    b.maxZ = Math.max(b.maxZ, building.center.z)
    b.count++
  }

  // 创建分区天气发射器
  for (const [zid, b] of zoneBounds) {
    b.cx /= b.count
    b.cz /= b.count
    const halfW = Math.max(10, (b.maxX - b.minX) / 2 + 8)
    const halfH = Math.max(10, (b.maxZ - b.minZ) / 2 + 8)

    weatherSystem.addZone(zid, { cx: b.cx, cz: b.cz, halfW, halfH })

    // 设置该分区初始天气
    const zone = getAllZones().find(z => z.id === zid)
    if (zone) {
      weatherSystem.setZoneWeather(zid, zone.weather)
    }
  }

  console.log(`🌤️ 已为 ${zoneBounds.size} 个分区创建独立天气发射器`)
}

// 创建分区天气指示器（地面彩色光环 + 点光源）
function createZoneIndicators() {
  if (!buildingMap) return

  // 计算每个分区的建筑中心点和边界
  const zoneCenters = new Map()
  const zoneBounds = new Map()

  for (const building of buildingMap.values()) {
    if (!building.zone || !building.center) continue
    const zid = building.zone.id
    if (!zoneCenters.has(zid)) {
      zoneCenters.set(zid, { sum: new THREE.Vector3(), count: 0 })
      zoneBounds.set(zid, { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity })
    }
    zoneCenters.get(zid).sum.add(building.center)
    zoneCenters.get(zid).count++
    const bounds = zoneBounds.get(zid)
    bounds.minX = Math.min(bounds.minX, building.center.x)
    bounds.maxX = Math.max(bounds.maxX, building.center.x)
    bounds.minZ = Math.min(bounds.minZ, building.center.z)
    bounds.maxZ = Math.max(bounds.maxZ, building.center.z)
  }

  for (const [zid, data] of zoneCenters) {
    const center = data.sum.divideScalar(data.count)
    const zone = getAllZones().find(z => z.id === zid)
    const color = WEATHER_COLORS[zone.weather] || 0xcccccc
    const bounds = zoneBounds.get(zid)

    // ★ 分区地面色块 — 发光边缘线框（替代实心矩形）
    const width = Math.max(20, bounds.maxX - bounds.minX + 15)
    const depth = Math.max(20, bounds.maxZ - bounds.minZ + 15)
    const planeGeo = new THREE.PlaneGeometry(width, depth)
    const edges = new THREE.EdgesGeometry(planeGeo)
    const lineMat = new THREE.LineBasicMaterial({
      color,
      transparent: true,
      opacity: 0.8,
    })
    const wireframe = new THREE.LineSegments(edges, lineMat)
    wireframe.rotation.x = -Math.PI / 2
    wireframe.position.set(center.x, 0.5, center.z)
    wireframe.name = `zone-plane-${zid}`
    scene.add(wireframe)
    zonePlanes.push({ zoneId: zid, mesh: wireframe, center, bounds: { width, depth } })

    // 光环
    const ringGeo = new THREE.TorusGeometry(5, 0.4, 16, 32)
    const ringMat = new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.5,
      depthWrite: false
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2
    ring.position.set(center.x, 0.1, center.z)
    ring.name = `zone-ring-${zid}`
    scene.add(ring)

    // 地面光斑
    const glowGeo = new THREE.CircleGeometry(4, 32)
    const glowMat = new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.15,
      side: THREE.DoubleSide,
      depthWrite: false
    })
    const glow = new THREE.Mesh(glowGeo, glowMat)
    glow.rotation.x = -Math.PI / 2
    glow.position.set(center.x, 0.06, center.z)
    glow.name = `zone-glow-${zid}`
    scene.add(glow)

    // 点光源
    const pointLight = new THREE.PointLight(color, 4, 50, 2)
    pointLight.position.set(center.x, 15, center.z)
    pointLight.name = `zone-light-${zid}`
    scene.add(pointLight)

    zoneIndicators.push({ zoneId: zid, ring, glow, light: pointLight, center, wireframe })
  }

  console.log(`🌈 创建了 ${zoneIndicators.length} 个分区天气指示器`)
}

// 更新分区天气指示器
function updateZoneIndicators() {
  for (const indicator of zoneIndicators) {
    const weather = getZoneWeather(indicator.zoneId)
    const color = WEATHER_COLORS[weather] || 0xcccccc
    indicator.ring.material.color.setHex(color)
    indicator.glow.material.color.setHex(color)
    if (indicator.wireframe) {
      indicator.wireframe.material.color.setHex(color)
    }
    if (indicator.light) {
      indicator.light.color.setHex(color)
      const intensityMap = { sunny: 5, cloudy: 3, overcast: 2, rainy: 1.5, heavy_rain: 1, thunderstorm: 0.8 }
      indicator.light.intensity = intensityMap[weather] || 10
    }
  }
}

function setZoneWeatherHandler(zoneId, weatherCode) {
  log.log('setZoneWeatherHandler', { zoneId, weather: weatherCode })
  setZoneWeather(zoneId, weatherCode)
  const zone = zoneList.value.find(z => z.id === zoneId)
  if (zone) zone.weather = weatherCode
  if (weatherSystem) weatherSystem.setZoneWeather(zoneId, weatherCode)
  updateLabelZoneInfo()
  emit('zoneWeatherChange', { zoneId, weather: weatherCode })
}

function setAllZoneWeathers(weatherMap) {
  log.log('setAllZoneWeathers', { zoneCount: Object.keys(weatherMap).length, weathers: weatherMap })
  for (const [zoneId, weather] of Object.entries(weatherMap)) {
    setZoneWeatherHandler(zoneId, weather)
  }
}

// 刷新标签中的分区天气信息（标签颜色随分区天气变化）
function updateLabelZoneInfo() {
  for (const label of visibleLabels.value) {
    const zone = getZoneByBuilding(label.name)
    if (zone) {
      const color = getWeatherHex(zone.weather)
      label.zoneColor = color
    }
  }
}

function getWeatherHex(code) {
  const hex = WEATHER_COLORS[code] || 0xcccccc
  return '#' + hex.toString(16).padStart(6, '0')
}

function getWeatherIcon(code) {
  const icons = { sunny: '☀️', cloudy: '⛅', overcast: '☁️', rainy: '🌧️', heavy_rain: '⛈️', thunderstorm: '🌩️', snow: '❄️' }
  return icons[code] || '⛅'
}

// 初始化分区标签
function initZoneLabels() {
  const labels = []
  for (const indicator of zoneIndicators) {
    const zone = getAllZones().find(z => z.id === indicator.zoneId)
    if (!zone) continue
    labels.push({
      id: zone.id,
      name: zone.name,
      weather: zone.weather,
      worldPos: indicator.center.clone(),
      style: {}
    })
  }
  visibleZoneLabels.value = labels
}

// 更新分区标签位置
function updateZoneLabelPositions() {
  if (!camera || !containerRef.value) return

  const cw = containerRef.value.clientWidth
  const ch = containerRef.value.clientHeight

  for (const zlabel of visibleZoneLabels.value) {
    const pos = zlabel.worldPos.clone()
    pos.y += 10  // 分区标签更高
    pos.project(camera)

    const rawX = (pos.x * 0.5 + 0.5) * cw
    const rawY = (-pos.y * 0.5 + 0.5) * ch

    const x = Math.round(rawX)
    const y = Math.round(rawY)

    if (pos.z > 1 || x < -150 || x > cw + 150 || y < -150 || y > ch + 150) {
      zlabel.style = { display: 'none' }
    } else {
      const dist = Math.round(camera.position.distanceTo(zlabel.worldPos))
      const opacity = Math.max(0, Math.min(1, 1 - (dist - 80) / 120))
      const scale = Math.max(0.6, Math.min(1.2, 1.2 - (dist - 60) / 180))

      const lastX = zlabel._x || 0; const lastY = zlabel._y || 0
      if (Math.abs(x - lastX) <= 2 && Math.abs(y - lastY) <= 2) continue
      zlabel._x = x; zlabel._y = y

      // 更新天气
      zlabel.weather = getZoneWeather(zlabel.id)

      zlabel.style = {
        display: 'flex',
        left: x + 'px',
        top: y + 'px',
        opacity,
        transform: `translate(-50%, -50%) scale(${scale})`
      }
    }
  }
}

// 一键切换所有分区天气
function switchWeather(code) {
  log.log('switchWeather', { code })
  currentWeather.value = code
  if (weatherSystem) weatherSystem.setWeather(code)
  for (const z of zoneList.value) {
    z.weather = code
    setZoneWeather(z.id, code)
  }
}

// ═══════════ 动画循环 ═══════════
function animate() {
  animationId = requestAnimationFrame(animate)

  const delta = Math.min(0.1, 1 / 60)
  const time = performance.now() * 0.001

  controls.update()

  if (weatherSystem) {
    weatherSystem.update(delta, time)
  }

  // 更新标签位置
  updateLabelPositions()
  updateZoneLabelPositions()

  // 更新分区天气指示器（微弱呼吸动画）
  updateZoneIndicators()
  const breathe = 1 + Math.sin(time * 0.5) * 0.06
  for (const indicator of zoneIndicators) {
    if (indicator.ring) indicator.ring.scale.setScalar(breathe)
    if (indicator.glow) indicator.glow.material.opacity = 0.08 + Math.sin(time * 0.7 + indicator.zoneId.charCodeAt(0)) * 0.03
  }

  composer.render()
}

// ═══════════ 窗口调整 ═══════════
function onResize() {
  if (!containerRef.value) return
  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  log.log('onResize', { w, h })
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  composer.setSize(w, h)
}

defineExpose({ setZoneWeatherHandler, setAllZoneWeathers, zoneList, switchWeather })
</script>

<style scoped>
.nju-scene {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  overflow: hidden;
}

.nju-scene canvas {
  display: block;
}

/* ── 加载界面 ── */
.loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10, 15, 30, 0.85);
  backdrop-filter: blur(12px);
}

.loading-card {
  text-align: center;
  padding: 32px 40px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #7ec8e3;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.loading-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-bottom: 12px;
}

.loading-bar-track {
  width: 200px;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
  margin: 0 auto 8px;
}

.loading-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #7ec8e3, #5a9ec0);
  border-radius: 2px;
  transition: width 0.3s;
}

.loading-pct {
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
}

/* ── 建筑标签 ── */
.labels-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.building-label {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  pointer-events: auto;
  transition: left 0.1s ease-out, top 0.1s ease-out, opacity 0.2s;
}

.building-label:hover {
  z-index: 20;
}

.label-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 6px rgba(126, 200, 227, 0.6);
}

.label-text {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(10, 10, 30, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  letter-spacing: 0.5px;
}

/* ── 分区天气调试面板 ── */
.zone-weather-panel {
  position: absolute;
  top: 60px;
  right: 8px;
  z-index: 30;
  background: rgba(10, 10, 30, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 10px 12px;
  max-height: calc(70vh - 140px);
  overflow-y: auto;
  font-size: 11px;
}

.zone-panel-title {
  color: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  margin-bottom: 6px;
  text-align: center;
  letter-spacing: 1px;
}

.zone-grid {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.zone-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.zone-label {
  width: 18px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  font-weight: 600;
  text-align: right;
}

.zone-select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  font-size: 11px;
  padding: 2px 4px;
  cursor: pointer;
  outline: none;
  width: 80px;
}

.zone-select option {
  background: #1a1a2e;
  color: #fff;
}

/* ── 分区标签 ── */
.zone-labels-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 15;
}

.zone-label {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 5px;
  pointer-events: none;
  transition: left 0.15s ease-out, top 0.15s ease-out, opacity 0.2s;
}

.zone-label-icon {
  font-size: 20px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.6));
}

.zone-label-text {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: rgba(10, 10, 40, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 4px 14px;
  border-radius: 14px;
  white-space: nowrap;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  letter-spacing: 0.8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}
</style>
