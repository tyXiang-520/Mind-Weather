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

    <!-- 分区天气调试面板 -->
    <div class="zone-weather-panel">
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

const props = defineProps({
  weather: { type: String, default: 'cloudy' }
})

const emit = defineEmits(['buildingClick', 'ready'])

// ═══════════ 天气色调映射 ═══════════
const WEATHER_COLORS = {
  sunny: 0xf0c040,
  cloudy: 0xc0c8d0,
  overcast: 0x888890,
  rainy: 0x5588aa,
  heavy_rain: 0x446688,
  thunderstorm: 0x554488
}

// ═══════════ 状态 ═══════════
const containerRef = ref(null)
const loading = ref(true)
const loadProgress = ref(0)
const currentWeather = ref(props.weather)
const visibleLabels = ref([])

const weatherTypes = [
  { code: 'sunny', icon: '☀️', label: '晴' },
  { code: 'cloudy', icon: '⛅', label: '多云' },
  { code: 'overcast', icon: '☁️', label: '阴' },
  { code: 'rainy', icon: '🌧️', label: '雨' },
  { code: 'heavy_rain', icon: '⛈️', label: '暴雨' },
  { code: 'thunderstorm', icon: '🌩️', label: '雷暴' }
]

// 分区列表（响应式，用于调试面板）
const zoneList = ref(getAllZones().map(z => ({ id: z.id, name: z.name, weather: z.weather })))

// ═══════════ Three.js 对象 ═══════════
let scene, camera, renderer, controls, composer, bloomPass
let weatherSystem, animationId
let gltfScene, buildingMap
let zoneIndicators = []  // 分区天气指示器

// ═══════════ 初始化 ═══════════
onMounted(() => {
  initScene()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  if (controls) controls.dispose()
  if (weatherSystem) weatherSystem.dispose()
  if (renderer) renderer.dispose()
  if (composer) composer.dispose()
})

// ═══════════ 场景初始化 ═══════════
function initScene() {
  const container = containerRef.value
  if (!container) return

  const w = container.clientWidth
  const h = container.clientHeight

  // ── 场景 ──
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x87CEEB) // 天蓝色背景
  scene.fog = new THREE.FogExp2(0xc8d0d8, 0.000025)

  // ── 相机 ──
  camera = new THREE.PerspectiveCamera(45, w / h, 1, 800)
  camera.position.set(80, 100, 80)
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
    new THREE.Vector2(w, h),
    0.25,  // strength
    0.5,   // radius
    0.6    // threshold — 更高=只有亮部发光
  )
  composer.addPass(bloomPass)

  // ── 光照 ──
  setupLighting()

  // ── OrbitControls ──
  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(5, 0, -30)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 15
  controls.maxDistance = 250
  controls.maxPolarAngle = Math.PI / 2.2
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.3
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
      console.error('❌ 模型加载失败:', error)
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

    const x = (pos.x * 0.5 + 0.5) * cw
    const y = (-pos.y * 0.5 + 0.5) * ch

    // 在相机后面或屏幕外隐藏
    if (pos.z > 1 || x < -50 || x > cw + 50 || y < -50 || y > ch + 50) {
      label.style = { display: 'none' }
    } else {
      const dist = camera.position.distanceTo(label.worldPos)
      // 缩短显示距离：80以内全显示，80-200渐隐，200以上隐藏
      const opacity = Math.max(0, Math.min(1, 1 - (dist - 80) / 120))
      const scale = Math.max(0.5, Math.min(1, 1 - (dist - 60) / 140))

      label.style = {
        display: 'flex',
        left: x + 'px',
        top: y + 'px',
        opacity,
        transform: `translate(-50%, -50%) scale(${scale})`
      }
    }
  }
}

// ═══════════ 交互 ═══════════
function onCanvasClick(event) {
  if (!camera || !gltfScene || !buildingMap) return

  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)

  // 对 GLB 场景中所有 mesh 做射线检测
  const meshes = []
  gltfScene.traverse((child) => {
    if (child.isMesh) meshes.push(child)
  })

  const intersects = raycaster.intersectObjects(meshes, false)
  if (intersects.length > 0) {
    // 从命中的 mesh 反向查找所属建筑
    const hitName = resolveHitBuilding(intersects[0].object)
    if (hitName && buildingMap.has(hitName)) {
      const building = buildingMap.get(hitName)
      const zone = building.zone
      emit('buildingClick', {
        name: building.name,
        displayName: building.displayName,
        center: building.center,
        zone: zone ? { id: zone.id, name: zone.name, weather: zone.weather } : null
      })
    }
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

  // 计算每个分区的建筑中心点平均值
  const zoneCenters = new Map()

  for (const building of buildingMap.values()) {
    if (!building.zone || !building.center) continue
    const zid = building.zone.id
    if (!zoneCenters.has(zid)) {
      zoneCenters.set(zid, { sum: new THREE.Vector3(), count: 0 })
    }
    zoneCenters.get(zid).sum.add(building.center)
    zoneCenters.get(zid).count++
  }

  for (const [zid, data] of zoneCenters) {
    const center = data.sum.divideScalar(data.count)
    const zone = getAllZones().find(z => z.id === zid)
    const color = WEATHER_COLORS[zone.weather] || 0xcccccc

    // 光环
    const ringGeo = new THREE.TorusGeometry(3, 0.25, 16, 32)
    const ringMat = new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.3,
      depthWrite: false
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2
    ring.position.set(center.x, 0.08, center.z)
    ring.name = `zone-ring-${zid}`
    scene.add(ring)

    // 地面光斑
    const glowGeo = new THREE.CircleGeometry(2.5, 32)
    const glowMat = new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.10,
      side: THREE.DoubleSide,
      depthWrite: false
    })
    const glow = new THREE.Mesh(glowGeo, glowMat)
    glow.rotation.x = -Math.PI / 2
    glow.position.set(center.x, 0.05, center.z)
    glow.name = `zone-glow-${zid}`
    scene.add(glow)

    // ★ 点光源 —— 微弱的区域色调
    const pointLight = new THREE.PointLight(color, 3, 40, 2)
    pointLight.position.set(center.x, 12, center.z)
    pointLight.name = `zone-light-${zid}`
    scene.add(pointLight)

    zoneIndicators.push({ zoneId: zid, ring, glow, light: pointLight, center })
  }

  console.log(`🌈 创建了 ${zoneIndicators.length} 个分区天气指示器（含点光源）`)
}

// 更新分区天气指示器
function updateZoneIndicators() {
  for (const indicator of zoneIndicators) {
    const weather = getZoneWeather(indicator.zoneId)
    const color = WEATHER_COLORS[weather] || 0xcccccc
    indicator.ring.material.color.setHex(color)
    indicator.glow.material.color.setHex(color)
    if (indicator.light) {
      indicator.light.color.setHex(color)
      // 晴天光源更强，雨天色温偏冷
      const intensityMap = { sunny: 5, cloudy: 3, overcast: 2, rainy: 1.5, heavy_rain: 1, thunderstorm: 0.8 }
      indicator.light.intensity = intensityMap[weather] || 10
    }
  }
}

function setZoneWeatherHandler(zoneId, weatherCode) {
  setZoneWeather(zoneId, weatherCode)
  // 更新响应式列表
  const zone = zoneList.value.find(z => z.id === zoneId)
  if (zone) zone.weather = weatherCode
  // 该分区独立切换天气粒子
  if (weatherSystem) weatherSystem.setZoneWeather(zoneId, weatherCode)
  updateLabelZoneInfo()
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

// 一键切换所有分区天气
function switchWeather(code) {
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
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  composer.setSize(w, h)
}

defineExpose({ switchWeather })
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
  transition: transform 0.08s;
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
  top: 8px;
  right: 8px;
  z-index: 30;
  background: rgba(10, 10, 30, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 10px 12px;
  max-height: 70vh;
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
</style>
