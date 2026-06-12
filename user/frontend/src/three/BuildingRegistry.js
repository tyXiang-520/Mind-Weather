/**
 * 建筑名称注册表
 *
 * 将 GLB 中的原始节点名映射到逻辑建筑名和显示名。
 *
 * 工作原理：
 *   1. GLB 加载后遍历所有节点
 *   2. 每个节点按规则归类到某个"逻辑建筑"
 *   3. 逻辑建筑用于：标签位置计算、点击识别、按分区归类
 */

import * as THREE from 'three'
import { ZONES } from './ZoneData.js'

// ═══════════════════════════════════════
//  已知建筑前缀 —— 用于将子部件归并到父建筑
//  （Blender 中拆分建模的建筑，如蒙民伟楼_0/_1/_2）
// ═══════════════════════════════════════

const BUILDING_PREFIXES = [
  '北大楼', '大礼堂', '图书馆', '蒙民伟楼', '逸夫管理科学楼',
  '吕志和游泳馆', '吕志和馆', '汉口路校门', '校门', 'NJU_',
]

// ═══════════════════════════════════════
//  子部件映射 —— 无意义名字 → 父建筑
// ═══════════════════════════════════════

const SUB_COMPONENT_MAP = {
  // ── 北大楼子部件 ──
  '北大楼_屋顶基座': '北大楼',
  '北大楼_屋顶主塔': '北大楼',
  '北大楼_门框': '北大楼',
  '北大楼_五角星': '北大楼',
  '北大楼_门扇_左': '北大楼',
  '北大楼_门扇_右': '北大楼',
  '北大楼_台阶': '北大楼',
  '北大楼_线脚_主塔檐口': '北大楼',
  '北大楼_线脚_门楣': '北大楼',
  '北大楼_藤蔓_L1': '北大楼',
  '北大楼_藤蔓_L2': '北大楼',
  '北大楼_藤蔓_R1': '北大楼',
  '北大楼_藤蔓_R2': '北大楼',
  '北大楼_藤蔓_B1': '北大楼',
  '北大楼_藤蔓叶子': '北大楼',
  '北大楼_藤蔓_正面斑块': '北大楼',
  '北大楼_窗户': '北大楼',

  // ── 图书馆子部件（名字较通用，精确匹配）──
  'body': '图书馆', 'body.001': '图书馆', 'body.002': '图书馆',
  '一楼 墙': '图书馆', '一楼 墙.001': '图书馆', '一楼 墙.002': '图书馆',
  '分房': '图书馆', '分房.001': '图书馆', '分房.002': '图书馆',
  '小分房': '图书馆', '小分房.001': '图书馆', '小分房.002': '图书馆',
  '门口挡板': '图书馆', '门口挡板.001': '图书馆', '门口挡板.002': '图书馆',
  '窗户外围': '图书馆', '窗户外围.001': '图书馆', '窗户外围.002': '图书馆',
  '左边窗框': '图书馆', '左边窗框.001': '图书馆', '左边窗框.002': '图书馆',
  '梁柱': '图书馆', '梁柱.001': '图书馆', '梁柱.002': '图书馆',
  '梁柱1': '图书馆', '梁柱1.001': '图书馆', '梁柱1.002': '图书馆',
  '梁柱2': '图书馆', '梁柱2.001': '图书馆', '梁柱2.002': '图书馆',
  'STAIR': '图书馆', 'STAIR.001': '图书馆', 'STAIR.002': '图书馆',
  'floor': '图书馆', 'floor.001': '图书馆', 'floor.002': '图书馆',
  'glass': '图书馆', 'glass.001': '图书馆', 'glass.002': '图书馆',
  'lift': '图书馆', 'lift.001': '图书馆', 'lift.002': '图书馆',
  'win': '图书馆', 'win.001': '图书馆', 'win.002': '图书馆',
  'win1': '图书馆', 'win1.001': '图书馆', 'win1.002': '图书馆',
  'win2': '图书馆', 'win2.001': '图书馆', 'win2.002': '图书馆',
  '窗户': '图书馆', '窗户.003': '图书馆', '窗户.004': '图书馆', '窗户.005': '图书馆', '窗户.006': '图书馆',
  'window': '图书馆', 'window.001': '图书馆', 'window.002': '图书馆', 'window.003': '图书馆',
  'window downside': '图书馆', 'window downside.001': '图书馆',
  'front window-前阳台': '图书馆', 'front window-前阳台.2': '图书馆',
  'front window-front window': '图书馆', 'front window-front window.001': '图书馆',
  '主框架-侧面': '图书馆', '主框架-侧面.001': '图书馆',
  '主框架-主框架': '图书馆', '主框架-主框架.001': '图书馆', '主框架-主框架.002': '图书馆', '主框架-主框架.003': '图书馆',
  '前阳台': '图书馆', '前阳台.001': '图书馆',
  '前阳台 下板-stair': '图书馆', '前阳台 下板-stair.001': '图书馆',
  '前阳台 下板-主框架': '图书馆', '前阳台 下板-主框架.001': '图书馆',

  // ── 校门子部件 ──
  '校门_南京大学': '汉口路校门',
  '校门_右字': '汉口路校门',
  '校门_左字': '汉口路校门',
  '校门_旗杆': '汉口路校门',
  '校门_右字_0': '汉口路校门', '校门_右字_1': '汉口路校门', '校门_右字_2': '汉口路校门', '校门_右字_3': '汉口路校门',
  '校门_左字_0': '汉口路校门', '校门_左字_1': '汉口路校门', '校门_左字_2': '汉口路校门', '校门_左字_3': '汉口路校门',
  '主立柱右': '汉口路校门',
  '主立柱左': '汉口路校门',
  '横梁_主梁': '汉口路校门', '横梁_主梁.001': '汉口路校门',
  '横梁_基座': '汉口路校门',
  '横梁_飞檐': '汉口路校门', '横梁_飞檐.001': '汉口路校门',

  // ── 运动场 ──
  'NJU_操场': '苏浙运动场',
  'NJU_篮球场': '体育场',

  // ── 吕志和游泳馆子部件 ──
  '底盘-底盘': '吕志和游泳馆', '底盘-底盘.001': '吕志和游泳馆',
  '底盘-底盘2': '吕志和游泳馆', '底盘-底盘2.001': '吕志和游泳馆',
  '扶手': '吕志和游泳馆', '扶手.001': '吕志和游泳馆',
  '支架1': '吕志和游泳馆', '支架1.001': '吕志和游泳馆',
  '支架2': '吕志和游泳馆', '支架2.001': '吕志和游泳馆',
  '楼梯底部': '吕志和游泳馆', '楼梯底部.001': '吕志和游泳馆',
  '宝石体': '吕志和游泳馆',
  '小枝': '吕志和游泳馆',
  '前阳台 下板-主框架.001': '吕志和游泳馆',
  '前阳台 下板-stair.001': '吕志和游泳馆',
  '前阳台.001': '吕志和游泳馆',
  '主框架-侧面.001': '吕志和游泳馆',
  'window downside.001': '吕志和游泳馆',
  'window.001': '吕志和游泳馆',
  'front window-前阳台.001': '吕志和游泳馆',
  'front window-front window.001': '吕志和游泳馆',
}

// ═══════════════════════════════════════
//  建筑显示名映射
// ═══════════════════════════════════════

const DISPLAY_NAME_MAP = {
  '人工微结构科学与技术协同创新中心': '创新中心',
  '后期服务集团': '后勤服务集团',
  '食堂': '南园餐厅',
}

// ═══════════════════════════════════════
//  排除模式 —— 不作为建筑标签
// ═══════════════════════════════════════

const EXCLUDE_PATTERNS = [
  /^ground$/i,
  /_road/i,
  /Tree/i,
  /榕树/,
  /小树/,
  /lamp/i,
  /balloon/i,
  /seagull/i,
  /flag/i,
  /car/i,
  /^草坛$/,
  /^馆$/,
  /^育$/,
  /^体$/,
]

// 纯 generic 名字（不含汉字且匹配常见模式）
function isGenericName(name) {
  if (/^(Cube|Circle|Plane|Vert|Icosphere|Object_|Mesh)\b/i.test(name)) return true
  if (/^(文本|柱体|Steps|Leaves|Door|Star|Tower)\b/i.test(name)) return true
  return false
}

// ═══════════════════════════════════════
//  核心函数
// ═══════════════════════════════════════

/**
 * 清理节点名：去掉 Blender .001 .002 后缀、尾随 .、_N 拆分后缀
 */
function cleanName(name) {
  return name
    .replace(/\.\d{3}$/, '')
    .replace(/\.+$/, '')
    .replace(/_\d+$/, '')
}

/**
 * 将 GLB 节点名映射到逻辑建筑名
 * @param {string} rawName
 * @returns {string|null} 逻辑建筑名，null 表示排除
 */
export function resolveBuildingName(rawName) {
  if (!rawName) return null
  const name = rawName.trim()
  if (!name) return null

  // 1. 精确匹配（包括 Blender .001 变体）
  if (SUB_COMPONENT_MAP[name]) {
    return SUB_COMPONENT_MAP[name]
  }

  // 2. 已知建筑前缀匹配（北大楼_xxx, 蒙民伟楼_0, 图书馆_xxx 等）
  for (const prefix of BUILDING_PREFIXES) {
    if (name.startsWith(prefix)) {
      return prefix === 'NJU_' ? '苏浙运动场' : prefix
    }
  }

  // 3. 排除
  if (isGenericName(name)) return null
  for (const p of EXCLUDE_PATTERNS) {
    if (p.test(name)) return null
  }

  // 4. 清理后作为独立建筑
  const cleaned = cleanName(name)
  if (cleaned.length <= 1) return null

  return cleaned
}

/**
 * 获取建筑显示名
 */
export function getDisplayName(logicalName) {
  return DISPLAY_NAME_MAP[logicalName] || logicalName
}

/**
 * 从 GLB scene 提取所有逻辑建筑
 * @param {THREE.Group} gltfScene
 * @returns {Map<string, {name:string, displayName:string, nodes:THREE.Object3D[], center:THREE.Vector3}>}
 */
export function extractBuildings(gltfScene) {
  // 从 ZoneData 构建白名单（104 栋确认建筑 + 汉口路校门）
  const validBuildings = new Set()
  for (const z of ZONES) {
    for (const b of z.buildings) {
      validBuildings.add(b)
    }
  }
  validBuildings.add('汉口路校门')

  const buildingMap = new Map()

  // 第一遍：收集所有属于建筑的节点
  gltfScene.traverse((node) => {
    const rawName = node.name || ''
    const logicalName = resolveBuildingName(rawName)
    if (!logicalName) return

    // 白名单过滤：不在确认名单里的不显示标签
    if (!validBuildings.has(logicalName)) return

    if (!buildingMap.has(logicalName)) {
      buildingMap.set(logicalName, {
        name: logicalName,
        displayName: getDisplayName(logicalName),
        nodes: [],
        center: null,
      })
    }

    buildingMap.get(logicalName).nodes.push(node)
  })

  // 第二遍：计算每个建筑的包围盒中心
  for (const building of buildingMap.values()) {
    const box = new THREE.Box3()
    for (const node of building.nodes) {
      if (node.isMesh && node.geometry) {
        node.updateWorldMatrix(true, false)
        box.expandByObject(node)
      }
    }
    if (!box.isEmpty()) {
      building.center = new THREE.Vector3()
      box.getCenter(building.center)
    } else {
      // Fallback：从节点位置取平均
      const avg = new THREE.Vector3()
      let count = 0
      for (const node of building.nodes) {
        const wp = new THREE.Vector3()
        node.getWorldPosition(wp)
        avg.add(wp)
        count++
      }
      building.center = count > 0 ? avg.divideScalar(count) : new THREE.Vector3()
    }
  }

  return buildingMap
}

export { SUB_COMPONENT_MAP, DISPLAY_NAME_MAP }
