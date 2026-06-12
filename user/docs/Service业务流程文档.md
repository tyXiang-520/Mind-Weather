# 心晴（MindWeather）Service 层业务流程文档

## 1. 服务总览

| 服务 | 职责 | 类型 |
|------|------|------|
| MoodService | 情绪投稿：接收文字 → 安全审核 → AI 识别 → 天气映射 → 持久化 | 核心编排 |
| EmotionAnalysisService | AI 情绪识别：调用 DeepSeek API 分析文本情绪 | 外部集成 |
| WeatherMappingService | 天气映射：情绪类型 → 天气状态（编码+图标+名称） | 纯逻辑 |
| AreaWeatherService | 区域天气聚合：统计某区域所有天气记录 → 主导天气 + 分布 | 纯计算 |
| MapDisplayService | 地图数据展示：为 3D/2D 地图提供全校和区域的聚合数据 | 数据读取 |

---

## 2. 模块职责

### 2.1 MoodService（情绪投稿服务）

**定位**：核心业务编排器，是整个投稿闭环的入口。

```
用户输入文字 → MoodService → 输出天气映射结果
```

**职责边界**：
- 参数校验（长度限制 500 字、非空）
- 敏感词过滤（委托 EmotionAnalysisService）
- 去重检测（SHA-256 哈希 + 24 小时窗口）
- 区域编码校验（查 campus_areas 表）
- AI 情绪识别编排（委托 EmotionAnalysisService）
- 天气映射编排（委托 WeatherMappingService）
- 数据持久化（mood_posts + emotion_tags + post_tags 三表写入）
- 投稿日志记录（只记 ID 和哈希，不记原文）

**核心方法**：

| 方法 | 说明 |
|------|------|
| `submitTextPost(userId, request)` | 投稿主流程（@Transactional） |
| `getMyPosts(userId, page, pageSize)` | 分页查询用户投稿历史 |
| `deletePost(userId, postId)` | 软删除投稿（作者权限校验） |
| `hashContent(content)` | SHA-256 内容哈希 |
| `isDuplicatePost(userId, hash)` | 24 小时内去重检测 |

### 2.2 EmotionAnalysisService（AI 情绪识别服务）

**定位**：外部 AI API 的适配层，封装 DeepSeek 调用细节。

```
文本 → EmotionAnalysisService → 情绪类型 + 置信度 + 标签
```

**职责边界**：
- System Prompt 构建（角色定义 + 6 种情绪分类约束 + JSON 格式要求）
- HTTP 通信（RestTemplate → DeepSeek Chat API）
- 响应解析（JSON 提取 + 字段校验 + 容错）
- 内容安全审核（AI 审核 + 本地回退）

**核心方法**：

| 方法 | 说明 |
|------|------|
| `analyze(text)` | 情绪分析主流程 → EmotionResultDTO |
| `containsSensitiveContent(text)` | 内容安全审核 |

**三层架构**：
```
analyze()                     ← 业务编排层
  ├── buildPrompt()           ← Prompt 工程
  ├── callDeepSeekApi()       ← HTTP 通信层（独立封装）
  └── parseAnalysisResult()   ← 解析层
```

**容错策略**：

| 异常场景 | 行为 |
|----------|------|
| 文本为 null/空 | 返回默认值（emotionType=平静） |
| API Key 未配置 | 返回模拟 JSON |
| 网络超时/连接失败 | 返回默认值 |
| JSON 解析失败 | 返回默认值 |
| 情绪类型不在候选列表 | 回退为"平静" |
| 标签不足 3 个 | 补足"校园生活" |

### 2.3 WeatherMappingService（天气映射服务）

**定位**：纯逻辑映射引擎，无外部依赖、无副作用。

```
情绪类型字符串 → WeatherMappingService → 天气编码 + 图标 + 名称
```

**职责边界**：
- 维护情绪→天气映射规则表（委托 WeatherTypeEnum）
- 正向映射：情绪 → 天气
- 反向查询：天气编码 → 天气信息
- 主导天气计算：从情绪分布中取最大占比

**核心方法**：

| 方法 | 说明 |
|------|------|
| `mapEmotionToWeather(emotionType)` | 情绪类型 → WeatherResultDTO |
| `getWeatherByCode(weatherCode)` | 天气编码 → WeatherResultDTO |
| `getAllMappingRules()` | 获取全部 6 条映射规则 |
| `calculateDominantWeather(distribution)` | 从情绪分布计算主导天气（Stream） |

**映射规则**：

```
开心 ──→ SUNNY        ──→ ☀️ 晴天
平静 ──→ CLOUDY       ──→ ⛅ 多云
压力 ──→ OVERCAST     ──→ ☁️ 阴天
焦虑 ──→ RAINY        ──→ 🌧️ 雨天
悲伤 ──→ HEAVY_RAIN   ──→ ⛈️ 暴雨
崩溃 ──→ THUNDERSTORM ──→ 🌩️ 雷暴
未匹配 ──→ CLOUDY（默认）
```

### 2.4 AreaWeatherService（区域天气聚合服务）

**定位**：纯统计计算引擎，对一组天气记录进行聚合分析。

```
List<WeatherResultDTO> → AreaWeatherService → 主导天气 + 分布 + 总数
```

**职责边界**：
- 天气频次统计（Stream groupingBy + counting）
- 主导天气判定（Stream max + comparingByValue）
- 天气分布占比计算（Stream collect + toMap）
- 主导情绪反查（天气编码 → 情绪类型）
- 空列表保护（返回 CLOUDY 默认值）

**核心方法**：

| 方法 | 说明 |
|------|------|
| `aggregate(weatherList)` | 聚合统计主流程 → AreaWeatherDTO |

**三段 Stream 管道**：

```
输入: [SUNNY, RAINY, SUNNY, CLOUDY, RAINY, SUNNY]  (6 条记录)

管道1: groupingBy(identity, counting())
       → {"sunny": 3, "rainy": 2, "cloudy": 1}

管道2: max(comparingByValue, thenComparingByKey)
       → 主导天气: {code="sunny", icon="☀️", name="晴天"}  (3次)

管道3: collect(toMap(key, count/total))
       → {"sunny": 0.50, "rainy": 0.33, "cloudy": 0.17}

反查: WEATHER_TO_EMOTION["sunny"] → "开心"

输出: AreaWeatherDTO {
    dominantWeather: {sunny, ☀️, 晴天},
    postCount: 6,
    weatherDistribution: {sunny:0.50, rainy:0.33, cloudy:0.17}
}
```

### 2.5 MapDisplayService（地图展示服务）

**定位**：前端 3D/2D 地图的只读数据源，聚合全校投稿数据。

```
数据库快照 → MapDisplayService → 首页地图 JSON / 区域列表 JSON
```

**职责边界**：
- 全校主导天气计算（按各区域投稿数加权）
- 今日总投稿数统计
- 全校热门标签 Top N
- 各区域天气详情组装
- 区域坐标列表（含边界数据）

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getOverview()` | 首页 3D 地图全区域天气总览 |
| `getAreaList()` | 区域坐标列表（地图初始化） |
| `getCampusMainWeather()` | 全校主导天气（投稿数加权） |
| `getTotalPostsToday()` | 今日全校投稿总数 |
| `getHotTags(topN)` | 全校热门标签 Top N |

---

## 3. 输入输出

### 3.1 数据流概览

```
                       ┌──────────────────┐
                       │   PostTextRequest │
                       │  content          │
                       │  areaCode         │
                       └────────┬─────────┘
                                │
                                ▼
              ┌─────────────────────────────────────┐
              │          MoodService                 │
              │          submitTextPost()            │
              └──────┬──────────────┬───────────────┘
                     │              │
         ┌───────────▼──┐    ┌─────▼──────────┐
         │ text         │    │ emotionType     │
         │              │    │                 │
         │ Emotion      │    │ Weather         │
         │ Analysis     │    │ Mapping         │
         │ Service      │    │ Service         │
         │              │    │                 │
         │ analyze()    │    │ mapEmotion      │
         │              │    │ ToWeather()     │
         └──────┬───────┘    └─────┬──────────┘
                │                  │
                ▼                  ▼
         EmotionResultDTO    WeatherResultDTO
         ┌──────────────┐    ┌──────────────┐
         │ emotionType  │    │ weatherCode  │
         │ emotionTag   │    │ weatherName  │
         │ confidence   │    │ weatherIcon  │
         │ tags         │    └──────────────┘
         └──────────────┘           │
                │                   │
                └────────┬──────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   PostTextResponse   │
              │  postId              │
              │  emotionType         │
              │  weatherCode         │
              │  weatherName         │
              │  weatherIcon         │
              │  tags                │
              └──────────────────────┘
```

### 3.2 各服务输入输出明细

| 服务 | 方法 | 输入 | 输出 |
|------|------|------|------|
| MoodService | `submitTextPost` | `Long userId` + `PostTextRequest` | `PostTextResponse` |
| MoodService | `getMyPosts` | `Long userId, int page, int pageSize` | `List<Map>` |
| MoodService | `deletePost` | `Long userId, Long postId` | `void` |
| EmotionAnalysisService | `analyze` | `String text` | `EmotionResultDTO` |
| EmotionAnalysisService | `containsSensitiveContent` | `String text` | `boolean` |
| WeatherMappingService | `mapEmotionToWeather` | `String emotionType` | `WeatherResultDTO` |
| WeatherMappingService | `getWeatherByCode` | `String weatherCode` | `WeatherResultDTO` |
| WeatherMappingService | `getAllMappingRules` | — | `List<Map>` |
| WeatherMappingService | `calculateDominantWeather` | `Map<String, Double>` | `WeatherResultDTO` |
| AreaWeatherService | `aggregate` | `List<WeatherResultDTO>` | `AreaWeatherDTO` |
| MapDisplayService | `getOverview` | — | `Map<String, Object>` |
| MapDisplayService | `getAreaList` | — | `Map<String, Object>` |
| MapDisplayService | `getCampusMainWeather` | — | `Map<String, String>` |
| MapDisplayService | `getTotalPostsToday` | — | `int` |
| MapDisplayService | `getHotTags` | `int topN` | `List<Map>` |

---

## 4. 调用关系

### 4.1 依赖图

```
                            ┌──────────────┐
                            │  Controller  │
                            └──────┬───────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
     ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
     │  MoodService   │  │ MapDisplay     │  │  UserSpace     │
     │  (核心编排)     │  │ Service        │  │  Service       │
     └───┬─────┬──────┘  └──────┬─────────┘  └──────┬─────────┘
         │     │                │                    │
    ┌────▼─┐ ┌─▼──────────┐ ┌──▼──────────┐        │
    │Emotion│ │Weather     │ │AreaWeather  │        │
    │Analysis│ │Mapping    │ │Service      │        │
    │Service │ │Service    │ │(聚合计算)    │        │
    │(AI调用)│ │(纯逻辑)    │ └─────────────┘        │
    └───────┘ └─┬──────────┘                        │
                │                                    │
                └────────────────────────────────────┘
```

### 4.2 调用矩阵

| 调用方 → 被调用方 | EmotionAnalysis | WeatherMapping | AreaWeather |
|-------------------|:---:|:---:|:---:|
| **MoodService** | ✓ | ✓ | — |
| **MapDisplayService** | — | ✓ | ✓ |
| **UserSpaceService** | — | ✓ | — |
| **AreaWeatherService** | — | — | — |

### 4.3 MoodService 调用链（投稿闭环）

```
MoodServiceImpl.submitTextPost()
  │
  ├─ [1] sanitizeContent(content)           // 清洗文本
  ├─ [2] emotionAnalysisService.containsSensitiveContent()  // 安全审核
  ├─ [3] hashContent() + isDuplicatePost()  // 去重检测
  ├─ [4] campusAreaMapper.selectByCode()    // 区域校验
  ├─ [5] emotionAnalysisService.analyze()   // AI 情绪识别
  │       └── callDeepSeekApi()             // HTTP 调用
  │       └── parseAnalysisResult()         // 解析 JSON
  ├─ [6] weatherMappingService.mapEmotionToWeather()  // 天气映射
  ├─ [7] moodPostMapper.insert()            // 写入投稿
  ├─ [8] emotionTagMapper + postTagMapper   // 标签处理
  └─ [9] 组装 PostTextResponse              // 返回结果
```

### 4.4 MapDisplayService 调用链（首页地图）

```
MapDisplayServiceImpl.getOverview()
  │
  ├─ [1] getCampusMainWeather()
  │       ├── areaWeatherService.getAllLatestSnapshots()
  │       ├── 按 postCount 加权统计
  │       └── weatherMappingService.getWeatherByCode()
  │
  ├─ [2] getTotalPostsToday()
  │       └── moodPostMapper.countByCreatedAtBetween()
  │
  ├─ [3] getHotTags(10)
  │       └── moodPostMapper.selectTopTagsAllAreas()
  │
  └─ [4] 组装各区域详情
          ├── campusAreaMapper.selectAll()
          ├── areaWeatherService.getAllLatestSnapshots()
          └── weatherMappingService.getWeatherByCode()
```

---

## 5. 数据流向

### 5.1 完整数据流（从用户输入到地图展示）

```
用户输入文字
     │
     ▼
PostTextRequest { content:"图书馆考试压力好大", areaCode:"library" }
     │
     ▼
┌────────────────────────────────────────────────────────────┐
│ MoodService.submitTextPost()                              │
│                                                            │
│  1. 安全审核 (containsSensitiveContent) ──── 违规 → 拒绝   │
│  2. 去重检测 (isDuplicatePost) ──────────── 重复 → 拒绝   │
│  3. 区域校验 (CampusAreaMapper) ─────────── 无效 → 拒绝   │
│                                                            │
│  4. AI 识别 ──────────────────────────────────────────┐   │
│     EmotionAnalysisService.analyze("图书馆考试压力")    │   │
│     → EmotionResultDTO {                              │   │
│         emotionType: "压力",                           │   │
│         emotionTag: "考试周",                          │   │
│         confidence: 0.87,                             │   │
│         tags: ["考试周","ddl","图书馆","通宵"]         │   │
│       }                                               │   │
│  5. 天气映射 ──────────────────────────────────────┐  │   │
│     WeatherMappingService.mapEmotionToWeather("压力")│  │   │
│     → WeatherResultDTO {                          │  │   │
│         weatherCode: "overcast",                  │  │   │
│         weatherName: "阴天",                       │  │   │
│         weatherIcon: "☁️"                          │  │   │
│       }                                           │  │   │
│  6. 持久化 ───────────────────────────────────────┘  │   │
│     mood_posts INSERT { userId, area_id, emotion,   │   │
│       weather_code, weather_icon, content_hash }     │   │
│     emotion_tags UPSERT (去重)                       │   │
│     post_tags INSERT (关联)                          │   │
│                                                      │   │
│  7. 响应                                              │   │
│     → PostTextResponse {                             │   │
│         postId: 42,                                  │   │
│         emotionType: "压力",                          │   │
│         weatherCode: "overcast",                     │   │
│         weatherName: "阴天",                          │   │
│         weatherIcon: "☁️",                            │   │
│         tags: ["考试周","ddl","图书馆","通宵"]        │   │
│       }                                              │   │
└──────────────────────────────────────────────────────┘   │
     │                                                     │
     ▼                                                     │
  前端展示"你的天气：阴天 ☁️"                               │
     │
     │  数据持久化后，定时任务异步执行：
     │
     ▼
┌────────────────────────────────────────────────────────┐
│ AreaWeatherService.aggregate(某区域所有WeatherResult)   │
│                                                        │
│  输入: [overcast, overcast, rainy, sunny, overcast, ...]│
│                                                        │
│  管道1: groupingBy + counting                          │
│    → {overcast:15, rainy:8, sunny:3, cloudy:2}         │
│                                                        │
│  管道2: max(comparingByValue)                          │
│    → 主导: overcast (15次, 53.6%)                      │
│                                                        │
│  管道3: toMap(count/total)                             │
│    → {overcast:0.54, rainy:0.29, sunny:0.11, ...}      │
│                                                        │
│  输出: AreaWeatherDTO {                                │
│    dominantWeather: {overcast, ☁️, 阴天},               │
│    postCount: 28,                                      │
│    weatherDistribution: {...}                          │
│  }                                                     │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│ MapDisplayService.getOverview()                        │
│                                                        │
│  读取所有区域快照 + 用户投稿数据                         │
│                                                        │
│  输出: {                                               │
│    campusMainWeather: {rainy, 🌧️, 雨天},               │
│    totalPostsToday: 156,                               │
│    hotTags: ["考试周","ddl","毕业季",...],             │
│    areas: [                                            │
│      { id:1, name:"图书馆", weatherIcon:"☁️",          │
│        dominantEmotion:"压力", postCount:42, ... },     │
│      { id:5, name:"操场", weatherIcon:"☀️",            │
│        dominantEmotion:"开心", postCount:18, ... }      │
│    ]                                                   │
│  }                                                     │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
              前端 3D 地图渲染
       上层: 天气粒子效果（云层/雨/晴）
       下层: 3D 校园建筑 + 天气图标
```

### 5.2 情绪→天气映射数据流

```
┌──────────────────────────────────────────────────┐
│                 WeatherTypeEnum                   │
│                                                  │
│  EMOTION_MAP (static final)                      │
│  ┌────────────┬──────────────┬──────┬──────┐    │
│  │ 情绪        │ 枚举          │ 编码  │ 图标  │    │
│  ├────────────┼──────────────┼──────┼──────┤    │
│  │ 开心        │ SUNNY        │sunny │ ☀️   │    │
│  │ 平静        │ CLOUDY       │cloudy│ ⛅   │    │
│  │ 压力        │ OVERCAST     │over..│ ☁️   │    │
│  │ 焦虑        │ RAINY        │rainy │ 🌧️  │    │
│  │ 悲伤        │ HEAVY_RAIN   │heavy │ ⛈️   │    │
│  │ 崩溃        │ THUNDERSTORM │thun..│ 🌩️  │    │
│  └────────────┴──────────────┴──────┴──────┘    │
│                                                  │
│  fromEmotion("压力") → OVERCAST                   │
│  fromCode("rainy")   → RAINY                      │
│  OVERCAST.toWeatherResult() → WeatherResultDTO    │
└──────────────────────────────────────────────────┘
```

### 5.3 DTO 转换链

```
PostTextRequest
  ├── content  ──→ EmotionAnalysisService.analyze()  ──→ EmotionResultDTO
  │                                                       ├── emotionType ──→ WeatherMappingService.mapEmotionToWeather()
  │                                                       │                       └──→ WeatherResultDTO
  │                                                       ├── emotionTag
  │                                                       ├── confidence
  │                                                       └── tags ──────────────────────────────┐
  │                                                                                              │
  └── areaCode ──→ CampusAreaMapper.selectByCode() ──→ CampusArea                                │
                                                                                                 │
  WeatherResultDTO ◄────────────────────────────────────────────────────────────────────────────┘
  │                                                                                              │
  ├── weatherCode  ──────────────────────────────────────────────────────────────────────────┐  │
  ├── weatherName                                                                             │  │
  └── weatherIcon                                                                             │  │
       │                                                                                      │  │
       └──→ PostTextResponse ◄────────────────────────────────────────────────────────────────┘  │
            ├── postId                                                                           │
            ├── emotionType ◄──── EmotionResultDTO.emotionType                                   │
            ├── weatherCode ◄──── WeatherResultDTO.weatherCode                                   │
            ├── weatherName ◄──── WeatherResultDTO.weatherName                                   │
            ├── weatherIcon ◄──── WeatherResultDTO.weatherIcon                                   │
            └── tags ◄─────────── EmotionResultDTO.tags ─────────────────────────────────────────┘
```

---

## 6. 关键设计决策

| 决策 | 原因 |
|------|------|
| EmotionAnalysisService 三层架构（编排/通信/解析） | 隔离 AI API 变更影响，通信层可独立替换 |
| WeatherMappingService 委托 WeatherTypeEnum | 映射规则与枚举内聚，新增天气只需改枚举文件 |
| AreaWeatherService 纯计算、无依赖 | 可独立单元测试，输入输出明确 |
| MoodService 不直接调用 AreaWeatherService | 投稿高频、聚合低频，应异步解耦（定时任务触发） |
| API 异常返回默认值而非抛异常 | 保证前端始终能渲染，用户体验不因 AI 服务中断 |
| 日志不记录投稿原文 | 用户隐私保护，仅记录 ID 和哈希 |

---

## 7. 扩展点

| 位置 | 扩展方式 | 场景 |
|------|----------|------|
| EmotionAnalysisService | 新增 `callQwenApi()` 方法 | 接入通义千问作为备选 AI |
| WeatherTypeEnum | 新增枚举值 + EMOTION_MAP 新增条目 | 新增情绪类型（如"期待→RAINBOW"） |
| AreaWeatherService | 新增 `aggregateWithTags()` | 聚合时加入标签维度统计 |
| MoodService | 注删策略模式替换 `containsSensitiveContent` | 接入专业内容安全服务 |
