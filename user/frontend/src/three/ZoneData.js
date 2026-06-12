/**
 * 分区数据 —— 校园建筑 → 12 个分区（A-L）的映射
 *
 * 每个分区有：
 *   - name: 分区名
 *   - buildings: 该分区包含的 GLB 逻辑建筑名列表
 *   - weather: 当前天气状态（mock，后续由后端算法驱动）
 *
 * "标签不合并，投稿合并"规则：逸夫馆Ⅰ/Ⅱ/Ⅲ区标签分开，投稿统一到"逸夫馆"
 */

// ═══════════════════════════════════════
//  投稿名 → GLB 逻辑建筑名映射
//  （处理"标签不合并，投稿合并"的情况）
// ═══════════════════════════════════════

const POST_TO_LABEL_MAP = {
  // 逸夫馆：标签分Ⅰ/Ⅱ/Ⅲ区，投稿统一
  '逸夫馆': ['逸夫馆Ⅰ区', '逸夫馆Ⅱ区1', '逸夫馆Ⅱ区2', '逸夫馆Ⅲ区1', '逸夫馆Ⅲ区2', '逸夫馆Ⅲ区3'],
  // 费彝民楼：标签分A/B栋
  '费彝民楼': ['费彝民楼A栋', '费彝民楼B栋'],
}

/**
 * 根据投稿名展开为所有 GLB 标签名
 */
function expandPostName(postName) {
  if (POST_TO_LABEL_MAP[postName]) {
    return POST_TO_LABEL_MAP[postName]
  }
  return [postName]
}

// ═══════════════════════════════════════
//  12 分区定义
// ═══════════════════════════════════════

const ZONES = [
  {
    id: 'A',
    name: '教学核心',
    buildings: ['教学楼', '新教学楼', '图书馆', '南教学楼', '蒙民伟楼', '科技馆', '校史博物馆', '小礼堂', '东南楼', '南楼'],
    weather: 'cloudy'  // mock 初始天气
  },
  {
    id: 'B',
    name: '历史核心',
    buildings: ['北大楼', '大礼堂', '西大楼', '东大楼', '辛壬楼', '戊己庚楼', '丙丁楼', '甲乙楼', '东北楼', '信息管理服务中心'],
    weather: 'sunny'
  },
  {
    id: 'C',
    name: '文科楼群',
    buildings: ['逸夫馆', '费彝民楼', '田家炳艺术学院', '逸夫管理科学楼', '南大出版社', '建良楼'],
    weather: 'cloudy'
  },
  {
    id: 'D',
    name: '运动场馆',
    buildings: ['苏浙运动场', '吕志和游泳馆'],
    weather: 'sunny'
  },
  {
    id: 'E',
    name: '理科/生活',
    buildings: ['西南楼', '知行楼', '树华楼', '声学楼', '声学西楼', '物理楼', '健忠楼', '低温实验楼', '李四光旧居', '罗根泽旧居', '赛珍珠故居', '创新中心', '斗鸡闸', '水电管理中心'],
    weather: 'overcast'
  },
  {
    id: 'F',
    name: '餐饮生活',
    buildings: ['南园餐厅', '教工食堂', '教育超市', '南园综合楼', '南大浴室'],
    weather: 'cloudy'
  },
  {
    id: 'G',
    name: '南园宿舍A',
    buildings: ['南园1舍', '南园2舍', '南园3舍', '南园4舍', '南园5舍', '南园6舍', '南园7舍', '南园17舍', '南园18舍', '南园19舍', '东苑宿舍', '校医院', '松林楼', '中山楼'],
    weather: 'rainy'
  },
  {
    id: 'H',
    name: '陶园/南园B',
    buildings: ['综合服务大厅', '陶园1舍', '陶园2舍', '陶园3舍', '陶园南楼', '南园15舍', '南园16舍'],
    weather: 'cloudy'
  },
  {
    id: 'I',
    name: '南园宿舍C',
    buildings: ['菜鸟驿站', '南园13舍', '南园14舍', '南园20舍', '南园21舍', '有园宾馆', '南园教学楼', '荟萃楼', '后勤服务集团', '校园110报警中心', '校园纪念品商店'],
    weather: 'sunny'
  },
  {
    id: 'J',
    name: '南园宿舍D',
    buildings: ['南园8舍', '南园11舍', '南园12舍', '外教公寓', '拉贝故居', '南苑宾馆一号楼', '南苑宾馆二号楼'],
    weather: 'overcast'
  },
  {
    id: 'K',
    name: '北区科研',
    buildings: ['工程管理学院', '天文楼', '协鑫楼', '平仓楼', '平仓楼北楼', '华龙楼1号', '华龙楼2号', '华龙楼3号'],
    weather: 'cloudy'
  },
  {
    id: 'L',
    name: '综合/其他',
    buildings: ['唐仲英楼', '安中楼', '曾宪梓楼', '实验楼', '科学楼', '中美文化研究中心', '西苑宾馆'],
    weather: 'sunny'
  }
]

// ═══════════════════════════════════════
//  查询 API
// ═══════════════════════════════════════

/**
 * 根据 GLB 逻辑建筑名查找所属分区
 */
function getZoneByBuilding(buildingLogicalName) {
  for (const zone of ZONES) {
    for (const b of zone.buildings) {
      const expanded = expandPostName(b)
      if (expanded.includes(buildingLogicalName)) {
        return zone
      }
      // 也检查投稿名本身
      if (b === buildingLogicalName) {
        return zone
      }
    }
  }
  return null
}

/**
 * 获取分区当前天气
 */
function getZoneWeather(zoneId) {
  const zone = ZONES.find(z => z.id === zoneId)
  return zone ? zone.weather : 'cloudy'
}

/**
 * 设置分区天气
 */
function setZoneWeather(zoneId, weather) {
  const zone = ZONES.find(z => z.id === zoneId)
  if (zone) zone.weather = weather
}

/**
 * 获取所有分区
 */
function getAllZones() {
  return ZONES
}

/**
 * 获取建筑所属分区的投稿名（用于提交投稿）
 */
function getPostZoneName(buildingLogicalName) {
  // 反向查 POST_TO_LABEL_MAP
  for (const [postName, labels] of Object.entries(POST_TO_LABEL_MAP)) {
    if (labels.includes(buildingLogicalName)) {
      return postName
    }
  }
  return buildingLogicalName
}

export {
  ZONES,
  POST_TO_LABEL_MAP,
  expandPostName,
  getZoneByBuilding,
  getZoneWeather,
  setZoneWeather,
  getAllZones,
  getPostZoneName
}
