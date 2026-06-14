/**
 * 分区天气粒子系统 v2 — 视觉增强版
 *
 * 每个分区独立一套粒子发射器，置于该分区建筑群正上方。
 * 支持：晴(阳光射线+光斑)、多云(大体积云)、阴(厚暗云)、雨(雨丝+水花)、暴雨(密集雨丝+水花)、雷暴(雨+闪电+屏幕闪光)、雪(飘落雪花)
 *
 * v2 改进：
 *   - 雨：用拉伸四边形代替圆点，模拟雨丝；新增地面水花
 *   - 云：体积增大 2-3 倍，分层浮动，更像真实云层
 *   - 晴天：新增阳光射线（god rays）+ 光斑放大
 *   - 闪电：新增可见闪电束（line geometry）+ 屏幕闪光
 *   - 雪：新增雪花效果，缓慢飘落+飘荡
 *   - 所有粒子从 y=120 相机距离可见
 */

import * as THREE from 'three'

// ═══════════════════════════════════════
//  天气配置 v2
// ═══════════════════════════════════════

const WEATHER_CFG = {
  sunny: {
    particles: 'sparkle',
    sparkleCount: 50,
    sparkleColor: 0xffcc44,
    sparkleSize: 1.8,
    sparkleSpeed: 0.15,
    cloudCount: 2,
    cloudOpacity: 0.2,
    sunRays: true,
    sunRayCount: 6,
    sunRayColor: 0xfff4d6,
    sunRayLength: 50,
  },
  cloudy: {
    particles: 'cloud',
    sparkleCount: 0,
    sparkleColor: 0xffffff,
    sparkleSize: 0,
    sparkleSpeed: 0,
    cloudCount: 6,
    cloudOpacity: 0.45,
    cloudScale: 0.9,
  },
  overcast: {
    particles: 'cloud',
    sparkleCount: 0,
    sparkleColor: 0x000000,
    sparkleSize: 0,
    sparkleSpeed: 0,
    cloudCount: 8,
    cloudOpacity: 0.55,
    cloudGray: true,
    cloudScale: 1.0,
  },
  rainy: {
    particles: 'rain',
    rainCount: 800,
    rainColor: 0x88bbee,
    rainSize: 1.2,
    rainSpeed: 4.0,
    rainLength: 3.0,
    splashCount: 40,
    cloudCount: 6,
    cloudOpacity: 0.5,
    cloudGray: true,
    cloudScale: 0.9,
  },
  heavy_rain: {
    particles: 'rain',
    rainCount: 1600,
    rainColor: 0x6699cc,
    rainSize: 1.5,
    rainSpeed: 6.0,
    rainLength: 4.5,
    splashCount: 80,
    cloudCount: 8,
    cloudOpacity: 0.6,
    cloudGray: true,
    cloudScale: 1.0,
  },
  thunderstorm: {
    particles: 'rain',
    rainCount: 1400,
    rainColor: 0x7799bb,
    rainSize: 1.4,
    rainSpeed: 7.0,
    rainLength: 4.0,
    splashCount: 60,
    cloudCount: 8,
    cloudOpacity: 0.65,
    cloudGray: true,
    cloudScale: 1.1,
    lightning: true,
    lightningBolt: true,
  },
  snow: {
    particles: 'snow',
    snowCount: 600,
    snowColor: 0xffffff,
    snowSize: 0.8,
    snowSpeed: 0.8,
    cloudCount: 5,
    cloudOpacity: 0.4,
    cloudGray: true,
    cloudScale: 0.8,
  },
}

// ═══════════════════════════════════════
//  单分区天气发射器 v2
// ═══════════════════════════════════════

class ZoneWeatherEmitter {
  constructor(scene, zoneId, bounds) {
    this.scene = scene
    this.zoneId = zoneId
    this.bounds = bounds
    this.currentWeather = 'cloudy'

    this.cloudGroup = new THREE.Group()
    this.cloudGroup.name = `clouds-${zoneId}`
    scene.add(this.cloudGroup)

    this.rainMesh = null
    this.splashGroup = null
    this.sparkleMesh = null
    this.sunRaysGroup = null
    this.snowMesh = null
    this.lightningLight = null
    this.lightningBolt = null
    this.lightningTimer = 0
    this.lightningNext = 5 + Math.random() * 15
    this.lightningFlash = false
    this.lightningDuration = 0
  }

  setWeather(weather) {
    this.currentWeather = weather
    const cfg = WEATHER_CFG[weather] || WEATHER_CFG.cloudy
    console.log(`🌤️ [${this.zoneId}区] 切换天气 → ${weather}`)

    this._removeRain()
    this._removeSplashes()
    this._removeSparkles()
    this._removeSunRays()
    this._removeSnow()
    this._removeClouds()
    this._removeLightning()
    this._removeLightningBolt()

    this._createClouds(cfg)
    if (cfg.particles === 'rain') {
      this._createRain(cfg)
      this._createSplashes(cfg)
    } else if (cfg.particles === 'sparkle') {
      this._createSparkles(cfg)
      if (cfg.sunRays) this._createSunRays(cfg)
    } else if (cfg.particles === 'snow') {
      this._createSnow(cfg)
    }
    if (cfg.lightning) this._createLightning()
    if (cfg.lightningBolt) this._createLightningBolt()
  }

  update(delta, time) {
    const cfg = WEATHER_CFG[this.currentWeather] || WEATHER_CFG.cloudy
    this._updateClouds(delta, time)
    if (cfg.particles === 'rain') {
      this._updateRain(delta, time)
      this._updateSplashes(delta, time)
    }
    if (cfg.particles === 'sparkle') {
      this._updateSparkles(delta, time)
      this._updateSunRays(delta, time)
    }
    if (cfg.particles === 'snow') this._updateSnow(delta, time)
    if (cfg.lightning) this._updateLightning(delta)
    if (cfg.lightningBolt) this._updateLightningBolt(delta)
  }

  dispose() {
    this._removeRain()
    this._removeSplashes()
    this._removeSparkles()
    this._removeSunRays()
    this._removeSnow()
    this._removeClouds()
    this._removeLightning()
    this._removeLightningBolt()
  }

  // ═══════════════════════════════════════
  //  云 v2 — 体积增大，分层浮动
  // ═══════════════════════════════════════

  _createClouds(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const scale = cfg.cloudScale || 1.0

    for (let i = 0; i < cfg.cloudCount; i++) {
      const cloud = new THREE.Group()
      const blobCount = 4 + Math.floor(Math.random() * 5)
      const color = cfg.cloudGray ? 0x778899 : 0xffffff

      for (let j = 0; j < blobCount; j++) {
        const r = (1.5 + Math.random() * 3) * scale
        const geo = new THREE.SphereGeometry(r, 8, 6)
        const mat = new THREE.MeshStandardMaterial({
          color,
          roughness: 0.85,
          transparent: true,
          opacity: cfg.cloudOpacity * (0.5 + Math.random() * 0.5),
          depthWrite: false,
        })
        const blob = new THREE.Mesh(geo, mat)
        blob.position.set(
          (Math.random() - 0.5) * 10 * scale,
          (Math.random() - 0.5) * 2,
          (Math.random() - 0.5) * 10 * scale
        )
        blob.scale.y = 0.25 + Math.random() * 0.3
        cloud.add(blob)
      }

      cloud.position.set(
        cx - halfW + Math.random() * halfW * 2,
        28 + Math.random() * 15,
        cz - halfH + Math.random() * halfH * 2
      )
      cloud.userData = {
        speed: 0.1 + Math.random() * 0.3,
        baseX: cloud.position.x,
        baseZ: cloud.position.z,
        baseY: cloud.position.y,
        phase: Math.random() * Math.PI * 2,
      }
      this.cloudGroup.add(cloud)
    }
  }

  _updateClouds(delta, time) {
    const { halfW, halfH, cx, cz } = this.bounds
    for (const cloud of this.cloudGroup.children) {
      const ud = cloud.userData
      cloud.position.x = ud.baseX + Math.sin(time * 0.08 + ud.phase) * halfW * 0.35
      cloud.position.z = ud.baseZ + Math.cos(time * 0.06 + ud.phase) * halfH * 0.35
      cloud.position.y = ud.baseY + Math.sin(time * 0.15 + ud.phase * 2) * 1.5

      if (cloud.position.x > cx + halfW + 25) cloud.position.x = cx - halfW - 25
      if (cloud.position.x < cx - halfW - 25) cloud.position.x = cx + halfW + 25
      if (cloud.position.z > cz + halfH + 25) cloud.position.z = cz - halfH - 25
      if (cloud.position.z < cz - halfH - 25) cloud.position.z = cz + halfH + 25
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

  // ═══════════════════════════════════════
  //  雨 v2 — 拉伸雨丝 + 地面水花
  // ═══════════════════════════════════════

  _createRain(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const areaW = halfW * 2 + 15
    const areaH = halfH * 2 + 15
    const areaTop = 20   // 雨丝从云层下方开始下落
    const rainLen = cfg.rainLength || 3.0

    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(cfg.rainCount * 6) // 2 vertices per raindrop (line)
    const velocities = new Float32Array(cfg.rainCount)

    for (let i = 0; i < cfg.rainCount; i++) {
      const x = cx - halfW - 8 + Math.random() * areaW
      const y = Math.random() * areaTop - 2  // y 从 -2 到 18
      const z = cz - halfH - 8 + Math.random() * areaH
      // 起点
      positions[i * 6] = x
      positions[i * 6 + 1] = y
      positions[i * 6 + 2] = z
      // 终点（向下拉伸）
      positions[i * 6 + 3] = x + (Math.random() - 0.5) * 0.3
      positions[i * 6 + 4] = y - rainLen
      positions[i * 6 + 5] = z + (Math.random() - 0.5) * 0.3
      velocities[i] = cfg.rainSpeed * (0.6 + Math.random() * 0.8)
    }

    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

    const mat = new THREE.LineBasicMaterial({
      color: cfg.rainColor,
      transparent: true,
      opacity: 0.6,
      linewidth: 1,
    })

    this.rainMesh = new THREE.LineSegments(geo, mat)
    this.rainMesh.name = `rain-${this.zoneId}`
    this.rainMesh.renderOrder = 999
    this.rainMesh.userData = { velocities, cfg, areaW, areaH, areaTop, cx, cz, halfW, halfH, rainLen }
    this.scene.add(this.rainMesh)
    console.log(`🌧️  ${this.zoneId}区: ${cfg.rainCount} 条雨丝`)
  }

  _updateRain(delta, time) {
    if (!this.rainMesh) return
    const pos = this.rainMesh.geometry.attributes.position.array
    const vels = this.rainMesh.userData.velocities
    const { areaW, areaH, areaTop, cx, cz, halfW, halfH, rainLen } = this.rainMesh.userData
    const count = pos.length / 6

    for (let i = 0; i < count; i++) {
      const baseIdx = i * 6
      // 起点下落
      pos[baseIdx + 1] -= vels[i] * delta
      pos[baseIdx + 4] = pos[baseIdx + 1] - rainLen

      // 微小水平漂移（风）
      const wind = Math.sin(time * 2 + i * 0.1) * 0.08
      pos[baseIdx] += wind
      pos[baseIdx + 3] += wind

      // 掉到地面后重置
      if (pos[baseIdx + 1] < 1) {
        const newY = areaTop + Math.random() * 8
        const newX = cx - halfW - 8 + Math.random() * areaW
        const newZ = cz - halfH - 8 + Math.random() * areaH
        pos[baseIdx] = newX
        pos[baseIdx + 1] = newY
        pos[baseIdx + 2] = newZ
        pos[baseIdx + 3] = newX + (Math.random() - 0.5) * 0.3
        pos[baseIdx + 4] = newY - rainLen
        pos[baseIdx + 5] = newZ + (Math.random() - 0.5) * 0.3
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

  // ─── 地面水花 ───
  _createSplashes(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const count = cfg.splashCount || 40
    this.splashGroup = new THREE.Group()
    this.splashGroup.name = `splashes-${this.zoneId}`

    for (let i = 0; i < count; i++) {
      const r = 0.3 + Math.random() * 0.5
      const geo = new THREE.RingGeometry(0.05, r, 8)
      const mat = new THREE.MeshBasicMaterial({
        color: 0xaaddff,
        transparent: true,
        opacity: 0,
        side: THREE.DoubleSide,
        depthWrite: false,
      })
      const splash = new THREE.Mesh(geo, mat)
      splash.rotation.x = -Math.PI / 2
      splash.position.set(
        cx - halfW + Math.random() * halfW * 2,
        0.15,
        cz - halfH + Math.random() * halfH * 2
      )
      splash.userData = {
        phase: Math.random() * Math.PI * 2,
        speed: 2 + Math.random() * 4,
        maxRadius: r,
        delay: Math.random() * 2,
      }
      this.splashGroup.add(splash)
    }
    this.scene.add(this.splashGroup)
  }

  _updateSplashes(delta, time) {
    if (!this.splashGroup) return
    for (const splash of this.splashGroup.children) {
      const ud = splash.userData
      const t = ((time * ud.speed + ud.delay) % 2) / 2 // 0→1 循环
      const scale = t * ud.maxRadius * 3
      splash.scale.set(scale, scale, scale)
      splash.material.opacity = t < 0.5 ? t * 0.6 : (1 - t) * 0.6
    }
  }

  _removeSplashes() {
    if (this.splashGroup) {
      this.splashGroup.traverse(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) child.material.dispose()
      })
      this.scene.remove(this.splashGroup)
      this.splashGroup = null
    }
  }

  // ═══════════════════════════════════════
  //  晴天光斑 v2 — 放大 + 阳光射线
  // ═══════════════════════════════════════

  _createSparkles(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const areaW = halfW * 2 + 10
    const areaH = halfH * 2 + 10

    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(cfg.sparkleCount * 3)
    const phases = new Float32Array(cfg.sparkleCount)

    for (let i = 0; i < cfg.sparkleCount; i++) {
      positions[i * 3] = cx - halfW - 5 + Math.random() * areaW
      positions[i * 3 + 1] = 10 + Math.random() * 30
      positions[i * 3 + 2] = cz - halfH - 5 + Math.random() * areaH
      phases[i] = Math.random() * Math.PI * 2
    }

    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

    const mat = new THREE.PointsMaterial({
      color: cfg.sparkleColor,
      size: cfg.sparkleSize,
      transparent: true,
      opacity: 0.9,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      sizeAttenuation: true,
    })

    this.sparkleMesh = new THREE.Points(geo, mat)
    this.sparkleMesh.name = `sparkle-${this.zoneId}`
    this.sparkleMesh.userData = { phases, sparkleSize: cfg.sparkleSize }
    this.scene.add(this.sparkleMesh)
  }

  _updateSparkles(delta, time) {
    if (!this.sparkleMesh) return
    const pos = this.sparkleMesh.geometry.attributes.position.array
    const phases = this.sparkleMesh.userData.phases
    const baseSize = this.sparkleMesh.userData.sparkleSize
    for (let i = 0; i < pos.length / 3; i++) {
      pos[i * 3 + 1] += Math.sin(time * 1.2 + phases[i]) * 0.04
      pos[i * 3] += Math.cos(time * 0.5 + phases[i]) * 0.02
    }
    this.sparkleMesh.geometry.attributes.position.needsUpdate = true
    // 剧烈闪烁，像阳光闪烁
    this.sparkleMesh.material.opacity = 0.5 + Math.sin(time * 2.0) * 0.4
    this.sparkleMesh.material.size = baseSize * (0.7 + Math.sin(time * 1.5) * 0.3)
  }

  _removeSparkles() {
    if (this.sparkleMesh) {
      this.scene.remove(this.sparkleMesh)
      this.sparkleMesh.geometry.dispose()
      this.sparkleMesh.material.dispose()
      this.sparkleMesh = null
    }
  }

  // ─── 阳光射线 ───
  _createSunRays(cfg) {
    const { cx, cz } = this.bounds
    this.sunRaysGroup = new THREE.Group()
    this.sunRaysGroup.name = `sunrays-${this.zoneId}`

    const rayCount = cfg.sunRayCount || 6
    const rayLen = cfg.sunRayLength || 50

    for (let i = 0; i < rayCount; i++) {
      const angle = (i / rayCount) * Math.PI * 2
      const geo = new THREE.BufferGeometry()
      const positions = new Float32Array(6) // 2 vertices

      // 从天空射向地面
      const sx = cx + Math.cos(angle) * 15
      const sz = cz + Math.sin(angle) * 15
      positions[0] = sx
      positions[1] = 55
      positions[2] = sz
      positions[3] = sx + Math.cos(angle) * 8
      positions[4] = 55 - rayLen
      positions[5] = sz + Math.sin(angle) * 8

      geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

      const mat = new THREE.LineBasicMaterial({
        color: cfg.sunRayColor || 0xfff4d6,
        transparent: true,
        opacity: 0.15,
        linewidth: 1,
      })

      const ray = new THREE.Line(geo, mat)
      ray.userData = { phase: Math.random() * Math.PI * 2, baseOpacity: 0.15 }
      this.sunRaysGroup.add(ray)
    }
    this.scene.add(this.sunRaysGroup)
  }

  _updateSunRays(delta, time) {
    if (!this.sunRaysGroup) return
    for (const ray of this.sunRaysGroup.children) {
      ray.material.opacity = ray.userData.baseOpacity * (0.5 + Math.sin(time * 0.5 + ray.userData.phase) * 0.5)
    }
  }

  _removeSunRays() {
    if (this.sunRaysGroup) {
      this.sunRaysGroup.traverse(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) child.material.dispose()
      })
      this.scene.remove(this.sunRaysGroup)
      this.sunRaysGroup = null
    }
  }

  // ═══════════════════════════════════════
  //  雪 v2
  // ═══════════════════════════════════════

  _createSnow(cfg) {
    const { cx, cz, halfW, halfH } = this.bounds
    const areaW = halfW * 2 + 15
    const areaH = halfH * 2 + 15
    const areaTop = 50

    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(cfg.snowCount * 3)
    const velocities = new Float32Array(cfg.snowCount)
    const phases = new Float32Array(cfg.snowCount)

    for (let i = 0; i < cfg.snowCount; i++) {
      positions[i * 3] = cx - halfW - 8 + Math.random() * areaW
      positions[i * 3 + 1] = 3 + Math.random() * areaTop
      positions[i * 3 + 2] = cz - halfH - 8 + Math.random() * areaH
      velocities[i] = cfg.snowSpeed * (0.3 + Math.random() * 0.7)
      phases[i] = Math.random() * Math.PI * 2
    }

    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

    const mat = new THREE.PointsMaterial({
      color: cfg.snowColor,
      size: cfg.snowSize,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
      sizeAttenuation: true,
    })

    this.snowMesh = new THREE.Points(geo, mat)
    this.snowMesh.name = `snow-${this.zoneId}`
    this.snowMesh.userData = { velocities, phases, cfg, areaW, areaH, areaTop, cx, cz, halfW, halfH }
    this.scene.add(this.snowMesh)
    console.log(`❄️  ${this.zoneId}区: ${cfg.snowCount} 片雪花`)
  }

  _updateSnow(delta, time) {
    if (!this.snowMesh) return
    const pos = this.snowMesh.geometry.attributes.position.array
    const vels = this.snowMesh.userData.velocities
    const phases = this.snowMesh.userData.phases
    const { areaW, areaH, areaTop, cx, cz, halfW, halfH } = this.snowMesh.userData
    const count = pos.length / 3

    for (let i = 0; i < count; i++) {
      pos[i * 3 + 1] -= vels[i] * delta
      // 飘荡
      pos[i * 3] += Math.sin(time * 0.8 + phases[i]) * 0.06
      pos[i * 3 + 2] += Math.cos(time * 0.6 + phases[i] * 1.3) * 0.06

      if (pos[i * 3 + 1] < 1) {
        pos[i * 3 + 1] = areaTop + Math.random() * 5
        pos[i * 3] = cx - halfW - 8 + Math.random() * areaW
        pos[i * 3 + 2] = cz - halfH - 8 + Math.random() * areaH
      }
    }
    this.snowMesh.geometry.attributes.position.needsUpdate = true
  }

  _removeSnow() {
    if (this.snowMesh) {
      this.scene.remove(this.snowMesh)
      this.snowMesh.geometry.dispose()
      this.snowMesh.material.dispose()
      this.snowMesh = null
    }
  }

  // ═══════════════════════════════════════
  //  闪电 v2 — 光 + 可见闪电束
  // ═══════════════════════════════════════

  _createLightning() {
    this.lightningLight = new THREE.PointLight(0xaaccff, 0, 150)
    this.lightningLight.position.set(this.bounds.cx, 35, this.bounds.cz)
    this.lightningLight.name = `lightning-${this.zoneId}`
    this.scene.add(this.lightningLight)
    this.lightningTimer = 0
    this.lightningNext = 2 + Math.random() * 5
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
        this.lightningLight.intensity = Math.max(0, this.lightningDuration * 50) * (0.3 + Math.random() * 1.4)
      }
    } else if (this.lightningTimer > this.lightningNext) {
      this.lightningFlash = true
      this.lightningDuration = 0.08 + Math.random() * 0.2
      this.lightningLight.position.set(
        this.bounds.cx + (Math.random() - 0.5) * this.bounds.halfW,
        30 + Math.random() * 18,
        this.bounds.cz + (Math.random() - 0.5) * this.bounds.halfH
      )
      this.lightningLight.intensity = 30 + Math.random() * 40

      // 更新闪电束位置
      if (this.lightningBolt) {
        this._updateBoltPosition()
      }
    }
  }

  _removeLightning() {
    if (this.lightningLight) {
      this.scene.remove(this.lightningLight)
      this.lightningLight = null
    }
    this.lightningFlash = false
  }

  // ─── 可见闪电束 ───
  _createLightningBolt() {
    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(30 * 3) // 最多 30 个点
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    geo.setDrawRange(0, 0)

    const mat = new THREE.LineBasicMaterial({
      color: 0xccddff,
      transparent: true,
      opacity: 0.9,
      linewidth: 2,
    })

    this.lightningBolt = new THREE.Line(geo, mat)
    this.lightningBolt.name = `lightning-bolt-${this.zoneId}`
    this.lightningBolt.visible = false
    this.scene.add(this.lightningBolt)
  }

  _updateBoltPosition() {
    if (!this.lightningBolt) return
    const { cx, cz, halfW, halfH } = this.bounds

    // 生成锯齿形闪电路径
    const startX = cx + (Math.random() - 0.5) * halfW
    const startZ = cz + (Math.random() - 0.5) * halfH
    const startY = 45 + Math.random() * 10
    const endY = 5 + Math.random() * 10
    const segments = 8 + Math.floor(Math.random() * 8)

    const pos = this.lightningBolt.geometry.attributes.position.array
    let x = startX, y = startY, z = startZ
    const dx = (Math.random() - 0.5) * 6
    const dz = (Math.random() - 0.5) * 6
    const dy = (endY - startY) / segments

    for (let i = 0; i < segments; i++) {
      pos[i * 3] = x
      pos[i * 3 + 1] = y
      pos[i * 3 + 2] = z
      x += dx + (Math.random() - 0.5) * 8
      y += dy
      z += dz + (Math.random() - 0.5) * 8
    }

    this.lightningBolt.geometry.attributes.position.needsUpdate = true
    this.lightningBolt.geometry.setDrawRange(0, segments)
    this.lightningBolt.visible = true
  }

  _updateLightningBolt(delta) {
    if (!this.lightningBolt) return
    if (this.lightningFlash && this.lightningDuration > 0) {
      this.lightningBolt.visible = true
      this.lightningBolt.material.opacity = 0.5 + Math.random() * 0.5
    } else {
      this.lightningBolt.visible = false
    }
  }

  _removeLightningBolt() {
    if (this.lightningBolt) {
      this.scene.remove(this.lightningBolt)
      this.lightningBolt.geometry.dispose()
      this.lightningBolt.material.dispose()
      this.lightningBolt = null
    }
  }
}

// ═══════════════════════════════════════
//  全局天气管理
// ═══════════════════════════════════════

export class WeatherSystem {
  constructor(scene) {
    this.scene = scene
    this.emitters = new Map()
    this.ambientLight = null
    this.sunLight = null
    this.bloomPass = null
  }

  addZone(zoneId, bounds) {
    if (this.emitters.has(zoneId)) return
    const emitter = new ZoneWeatherEmitter(this.scene, zoneId, bounds)
    this.emitters.set(zoneId, emitter)
    return emitter
  }

  setZoneWeather(zoneId, weather) {
    const emitter = this.emitters.get(zoneId)
    if (emitter) emitter.setWeather(weather)
  }

  setWeather(weather) {
    for (const emitter of this.emitters.values()) {
      emitter.setWeather(weather)
    }
  }

  init() {}

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
