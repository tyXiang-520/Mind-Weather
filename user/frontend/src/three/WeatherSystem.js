/**
 * 分区天气粒子系统
 *
 * 每个分区独立一套粒子发射器，置于该分区建筑群正上方。
 * 支持：晴(光斑)、多云(云朵)、阴(厚云)、雨、暴雨、雷暴(雨+闪电)
 */

import * as THREE from 'three'

// ═══════════════════════════════════════
//  天气配置
// ═══════════════════════════════════════

const WEATHER_CFG = {
  sunny:        { particles: 'sparkle', sparkleCount: 80,  sparkleColor: 0xffe8a0, sparkleSize: 0.18, sparkleSpeed: 0.25, cloudCount: 3,  cloudOpacity: 0.3 },
  cloudy:       { particles: 'cloud',   sparkleCount: 0,   sparkleColor: 0xffffff, sparkleSize: 0,    sparkleSpeed: 0,    cloudCount: 10, cloudOpacity: 0.55 },
  overcast:     { particles: 'cloud',   sparkleCount: 0,   sparkleColor: 0x000000, sparkleSize: 0,    sparkleSpeed: 0,    cloudCount: 16, cloudOpacity: 0.7,  cloudGray: true },
  rainy:        { particles: 'rain',    rainCount: 500,    rainColor: 0x6699cc,    rainSize: 0.35,    rainSpeed: 3.5,    cloudCount: 10, cloudOpacity: 0.6,  cloudGray: true },
  heavy_rain:   { particles: 'rain',    rainCount: 1000,   rainColor: 0x4488bb,    rainSize: 0.3,     rainSpeed: 5.5,    cloudCount: 14, cloudOpacity: 0.8,  cloudGray: true },
  thunderstorm: { particles: 'rain',    rainCount: 900,    rainColor: 0x5577bb,    rainSize: 0.35,    rainSpeed: 6.0,    cloudCount: 16, cloudOpacity: 0.85, cloudGray: true, lightning: true },
}

// ═══════════════════════════════════════
//  单分区天气发射器
// ═══════════════════════════════════════

class ZoneWeatherEmitter {
  constructor(scene, zoneId, bounds) {
    this.scene = scene
    this.zoneId = zoneId
    this.bounds = bounds          // { cx, cz, halfW, halfH }
    this.currentWeather = 'cloudy'

    this.cloudGroup = new THREE.Group()
    this.cloudGroup.name = `clouds-${zoneId}`
    scene.add(this.cloudGroup)

    this.rainMesh = null
    this.sparkleMesh = null
    this.lightningLight = null
    this.lightningTimer = 0
    this.lightningNext = 5 + Math.random() * 15
    this.lightningFlash = false
    this.lightningDuration = 0
  }

  /** 切换此分区天气 */
  setWeather(weather) {
    this.currentWeather = weather
    const cfg = WEATHER_CFG[weather] || WEATHER_CFG.cloudy
    console.log(`🌤️ [${this.zoneId}区] 切换天气 → ${weather} (粒子:${cfg.particles}, 云:${cfg.cloudCount})`)

    // 清理旧粒子
    this._removeRain()
    this._removeSparkles()
    this._removeClouds()
    this._removeLightning()

    // 创建新粒子
    this._createClouds(cfg)
    if (cfg.particles === 'rain') {
      this._createRain(cfg)
    } else if (cfg.particles === 'sparkle') {
      this._createSparkles(cfg)
    }
    if (cfg.lightning) {
      this._createLightning()
    }
  }

  /** 每帧更新 */
  update(delta, time) {
    const cfg = WEATHER_CFG[this.currentWeather] || WEATHER_CFG.cloudy
    this._updateClouds(delta, time)
    if (cfg.particles === 'rain') this._updateRain(delta, time)
    if (cfg.particles === 'sparkle') this._updateSparkles(delta, time)
    if (cfg.lightning) this._updateLightning(delta)
  }

  dispose() {
    this._removeRain()
    this._removeSparkles()
    this._removeClouds()
    this._removeLightning()
  }

  // ─── 云 ───
  _createClouds(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    for (let i = 0; i < cfg.cloudCount; i++) {
      const cloud = new THREE.Group()
      const blobCount = 4 + Math.floor(Math.random() * 6)
      const color = cfg.cloudGray ? 0x8899aa : 0xffffff

      for (let j = 0; j < blobCount; j++) {
        const r = 1.5 + Math.random() * 4
        const geo = new THREE.SphereGeometry(r, 8, 6)
        const mat = new THREE.MeshStandardMaterial({
          color, roughness: 0.9, transparent: true,
          opacity: cfg.cloudOpacity * (0.5 + Math.random() * 0.5),
          depthWrite: false
        })
        const blob = new THREE.Mesh(geo, mat)
        blob.position.set(
          (Math.random() - 0.5) * 10,
          (Math.random() - 0.5) * 2,
          (Math.random() - 0.5) * 10
        )
        blob.scale.y = 0.3 + Math.random() * 0.3
        cloud.add(blob)
      }

      cloud.position.set(
        cx - halfW + Math.random() * halfW * 2,
        18 + Math.random() * 20,
        cz - halfH + Math.random() * halfH * 2
      )
      cloud.userData = {
        speed: 0.15 + Math.random() * 0.4,
        baseX: cloud.position.x,
        baseZ: cloud.position.z,
        phase: Math.random() * Math.PI * 2
      }
      this.cloudGroup.add(cloud)
    }
  }

  _updateClouds(delta, time) {
    const { halfW, halfH, cx, cz } = this.bounds
    for (const cloud of this.cloudGroup.children) {
      const ud = cloud.userData
      cloud.position.x = ud.baseX + Math.sin(time * 0.12 + ud.phase) * halfW * 0.4
      cloud.position.z = ud.baseZ + Math.cos(time * 0.1 + ud.phase) * halfH * 0.4
      // 循环包裹
      if (cloud.position.x > cx + halfW + 20) cloud.position.x = cx - halfW - 20
      if (cloud.position.x < cx - halfW - 20) cloud.position.x = cx + halfW + 20
      if (cloud.position.z > cz + halfH + 20) cloud.position.z = cz - halfH - 20
      if (cloud.position.z < cz - halfH - 20) cloud.position.z = cz + halfH + 20
    }
  }

  _removeClouds() {
    while (this.cloudGroup.children.length) {
      const c = this.cloudGroup.children[0]
      c.traverse(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) child.material.dispose()
      })
      this.cloudGroup.remove(c)
    }
  }

  // ─── 雨 ───
  _createRain(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const areaW = halfW * 2 + 10
    const areaH = halfH * 2 + 10
    const areaTop = 45

    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(cfg.rainCount * 3)
    const velocities = new Float32Array(cfg.rainCount)

    for (let i = 0; i < cfg.rainCount; i++) {
      positions[i * 3]     = cx - halfW - 5 + Math.random() * areaW
      positions[i * 3 + 1] = 5 + Math.random() * areaTop
      positions[i * 3 + 2] = cz - halfH - 5 + Math.random() * areaH
      velocities[i] = cfg.rainSpeed * (0.5 + Math.random() * 1.0)
    }

    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

    // 使用细长的雨丝（不是圆点）
    const mat = new THREE.PointsMaterial({
      color: cfg.rainColor,
      size: cfg.rainSize,
      transparent: true,
      opacity: 0.7,
      blending: THREE.NormalBlending,
      depthWrite: false
    })

    this.rainMesh = new THREE.Points(geo, mat)
    this.rainMesh.name = `rain-${this.zoneId}`
    this.rainMesh.renderOrder = 999
    this.rainMesh.material.depthTest = false
    this.rainMesh.userData = { velocities, cfg, areaW, areaH, areaTop, cx, cz, halfW, halfH }
    this.scene.add(this.rainMesh)
    console.log(`🌧️  ${this.zoneId}区: ${cfg.rainCount} 个雨滴, 范围 ${areaW.toFixed(0)}×${areaH.toFixed(0)}, 高度 ${areaTop}`)
  }

  _updateRain(delta, time) {
    if (!this.rainMesh) return
    const pos = this.rainMesh.geometry.attributes.position.array
    const vels = this.rainMesh.userData.velocities
    const { areaW, areaH, areaTop, cx, cz, halfW, halfH } = this.rainMesh.userData
    const count = pos.length / 3

    for (let i = 0; i < count; i++) {
      pos[i * 3 + 1] -= vels[i] * delta
      // 微小的水平漂移
      pos[i * 3] += Math.sin(time * 3 + i) * 0.03

      if (pos[i * 3 + 1] < 2) {
        pos[i * 3 + 1] = areaTop + Math.random() * 10
        pos[i * 3] = cx - halfW - 5 + Math.random() * areaW
        pos[i * 3 + 2] = cz - halfH - 5 + Math.random() * areaH
      }
    }
    this.rainMesh.geometry.attributes.position.needsUpdate = true
  }

  _removeRain() {
    if (this.rainMesh) {
      this.scene.remove(this.rainMesh)
      this.rainMesh.geometry.dispose()
      this.rainMesh.material.dispose()
      this.rainMesh = null
    }
  }

  // ─── 晴天光斑 ───
  _createSparkles(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const areaW = halfW * 2 + 10
    const areaH = halfH * 2 + 10

    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(cfg.sparkleCount * 3)
    const phases = new Float32Array(cfg.sparkleCount)

    for (let i = 0; i < cfg.sparkleCount; i++) {
      positions[i * 3]     = cx - halfW - 5 + Math.random() * areaW
      positions[i * 3 + 1] = 6 + Math.random() * 30
      positions[i * 3 + 2] = cz - halfH - 5 + Math.random() * areaH
      phases[i] = Math.random() * Math.PI * 2
    }

    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

    const mat = new THREE.PointsMaterial({
      color: cfg.sparkleColor,
      size: cfg.sparkleSize,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })

    this.sparkleMesh = new THREE.Points(geo, mat)
    this.sparkleMesh.name = `sparkle-${this.zoneId}`
    this.sparkleMesh.userData = { phases }
    this.scene.add(this.sparkleMesh)
  }

  _updateSparkles(delta, time) {
    if (!this.sparkleMesh) return
    const pos = this.sparkleMesh.geometry.attributes.position.array
    const phases = this.sparkleMesh.userData.phases
    for (let i = 0; i < pos.length / 3; i++) {
      pos[i * 3 + 1] += Math.sin(time * 1.5 + phases[i]) * 0.05
      pos[i * 3] += Math.cos(time * 0.7 + phases[i]) * 0.03
    }
    this.sparkleMesh.geometry.attributes.position.needsUpdate = true

    // 闪烁
    this.sparkleMesh.material.opacity = 0.4 + Math.sin(time * 1.2) * 0.2
  }

  _removeSparkles() {
    if (this.sparkleMesh) {
      this.scene.remove(this.sparkleMesh)
      this.sparkleMesh.geometry.dispose()
      this.sparkleMesh.material.dispose()
      this.sparkleMesh = null
    }
  }

  // ─── 闪电 ───
  _createLightning() {
    this.lightningLight = new THREE.PointLight(0xaaccff, 0, 120)
    this.lightningLight.position.set(this.bounds.cx, 30, this.bounds.cz)
    this.lightningLight.name = `lightning-${this.zoneId}`
    this.scene.add(this.lightningLight)
    this.lightningTimer = 0
    this.lightningNext = 2 + Math.random() * 5
    console.log(`⚡ ${this.zoneId}区: 闪电系统就绪`)
  }

  _updateLightning(delta) {
    if (!this.lightningLight) return
    this.lightningTimer += delta

    if (this.lightningFlash) {
      this.lightningDuration -= delta
      if (this.lightningDuration <= 0) {
        this.lightningLight.intensity = 0
        this.lightningFlash = false
        this.lightningNext = 2 + Math.random() * 8
        this.lightningTimer = 0
      } else {
        // 随机闪烁
        this.lightningLight.intensity = Math.max(0, this.lightningDuration * 40) * (0.3 + Math.random() * 1.4)
      }
    } else if (this.lightningTimer > this.lightningNext) {
      this.lightningFlash = true
      this.lightningDuration = 0.1 + Math.random() * 0.25
      this.lightningLight.position.set(
        this.bounds.cx + (Math.random() - 0.5) * this.bounds.halfW,
        28 + Math.random() * 15,
        this.bounds.cz + (Math.random() - 0.5) * this.bounds.halfH
      )
      this.lightningLight.intensity = 25 + Math.random() * 30
    }
  }

  _removeLightning() {
    if (this.lightningLight) {
      this.scene.remove(this.lightningLight)
      this.lightningLight = null
    }
    this.lightningFlash = false
  }
}

// ═══════════════════════════════════════
//  全局天气管理
// ═══════════════════════════════════════

export class WeatherSystem {
  constructor(scene) {
    this.scene = scene
    this.emitters = new Map()   // zoneId → ZoneWeatherEmitter
    this.ambientLight = null
    this.sunLight = null
    this.bloomPass = null
  }

  /**
   * 为一个分区创建天气发射器
   * @param {string} zoneId
   * @param {{ cx:number, cz:number, halfW:number, halfH:number }} bounds
   */
  addZone(zoneId, bounds) {
    if (this.emitters.has(zoneId)) return
    const emitter = new ZoneWeatherEmitter(this.scene, zoneId, bounds)
    this.emitters.set(zoneId, emitter)
    return emitter
  }

  /** 设置某分区天气 */
  setZoneWeather(zoneId, weather) {
    const emitter = this.emitters.get(zoneId)
    if (emitter) emitter.setWeather(weather)
  }

  /** 兼容旧 API：设置所有分区天气 */
  setWeather(weather) {
    for (const emitter of this.emitters.values()) {
      emitter.setWeather(weather)
    }
  }

  /** 初始化（兼容旧调用，实际由 addZone 按需创建） */
  init() {}

  /** 每帧更新所有分区 */
  update(delta, time) {
    for (const emitter of this.emitters.values()) {
      emitter.update(delta, time)
    }
  }

  dispose() {
    for (const emitter of this.emitters.values()) {
      emitter.dispose()
    }
    this.emitters.clear()
  }
}

export { ZoneWeatherEmitter, WEATHER_CFG }
