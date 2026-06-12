# 心晴（MindWeather）API 文档 v3.0

> 统一后端接口文档。所有接口已实现，前端可直接联调。
> Base URL: `http://localhost:8080`

---

## 1. 基础规范

| 项目 | 规范 |
|------|------|
| 协议 | HTTP |
| 路径前缀 | `/api/v1/` |
| 数据格式 | JSON |
| 编码 | UTF-8 |
| 认证方式 | `Authorization: Bearer <token>` |
| HTTP 状态码 | **永远返回 200**，业务状态看 `body.code` |

---

## 2. 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 业务状态码，0 = 成功 |
| message | string | 状态描述 |
| data | object / null | 响应数据 |

---

## 3. 错误码全集

### 通用错误码

| code | message | 说明 |
|------|---------|------|
| 0 | success | 成功 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未登录 | 未携带有效 JWT |
| 403 | 无权限 | 无权访问该资源 |
| 404 | 资源不存在 | 请求的资源未找到 |
| 500 | 系统错误 | 服务器内部错误 |

### 业务层错误码（1001-1999）

| code | message | 说明 |
|------|---------|------|
| 1001 | AI分析失败 | AI 情绪分析服务异常 |
| 1002 | 敏感词违规 | 输入内容含敏感词 |

### 用户层错误码（2001-2999）

| code | message | 说明 |
|------|---------|------|
| 2001 | 邮箱已注册 | 注册邮箱已被占用 |
| 2002 | 用户不存在 | 登录邮箱未注册 |
| 2003 | 密码错误 | 登录密码不匹配 |
| 2004 | Token已过期 | JWT 超过 24 小时有效期 |
| 2005 | Token无效 | JWT 格式非法或被篡改 |

---

## 4. 认证接口

### 4.1 注册

```
POST /api/v1/auth/register
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| email | string | 是 | 邮箱地址 |
| password | string | 是 | 密码，明文传输，服务端 BCrypt 加密 |

**成功响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "user_38472",
    "token": "mock-token-..."
  }
}
```

**错误响应 — 邮箱已注册**

```json
{ "code": 2001, "message": "邮箱已注册", "data": null }
```

---

### 4.2 登录

```
POST /api/v1/auth/login
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| email | string | 是 | 邮箱地址 |
| password | string | 是 | 密码 |

**成功响应**

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "user_38472",
    "token": "mock-token-..."
  }
}
```

**错误响应**

```json
{ "code": 2002, "message": "用户不存在", "data": null }
{ "code": 2003, "message": "密码错误", "data": null }
```

---

### 4.3 获取个人资料

```
GET /api/v1/user/profile
Authorization: Bearer <token>
```

**成功响应**

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "nickname": "小太阳",
    "email": "test@example.com"
  }
}
```

---

## 5. 情绪投稿接口

### 5.1 提交文字投稿

```
POST /api/v1/posts/text
Authorization: Bearer <token>
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| content | string | 是 | 投稿文字，1-500 字 |
| areaCode | string | 是 | 区域编码，见 [校园区域编码](#8-校园区域编码) |

**成功响应**

```json
{
  "code": 0,
  "data": {
    "postId": 1,
    "emotionType": "开心",
    "weatherCode": "sunny",
    "weatherName": "晴天",
    "weatherIcon": "☀️",
    "tags": ["好心情", "快乐时光", "校园生活"]
  }
}
```

**错误响应**

```json
{ "code": 400, "message": "投稿内容不能为空", "data": null }
{ "code": 400, "message": "无效的区域编码: xxx", "data": null }
{ "code": 1002, "message": "敏感词违规", "data": null }
```

---

### 5.2 查询我的投稿

```
GET /api/v1/posts/my?page=1&pageSize=10
Authorization: Bearer <token>
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|:--:|------|
| page | int | 1 | 页码 |
| pageSize | int | 10 | 每页条数 |

**成功响应**

```json
{
  "code": 0,
  "data": [
    {
      "postId": 1,
      "content": "今天很开心",
      "emotionType": "开心",
      "weatherCode": "sunny",
      "weatherIcon": "☀️",
      "weatherName": "晴天",
      "areaCode": "library",
      "areaName": "图书馆",
      "tags": ["好心情", "快乐时光", "校园生活"],
      "createdAt": "2026-06-01T12:00:00"
    }
  ]
}
```

---

### 5.3 删除投稿

```
DELETE /api/v1/posts/{postId}
Authorization: Bearer <token>
```

**成功响应**

```json
{ "code": 0, "message": "success", "data": null }
```

**错误响应**

```json
{ "code": 400, "message": "投稿不存在", "data": null }
{ "code": 400, "message": "无权删除他人的投稿", "data": null }
```

---

## 6. 地图接口

### 6.1 全区域天气总览

```
GET /api/v1/map/overview
```

无需认证。

**成功响应**

```json
{
  "code": 0,
  "data": {
    "campusMainWeather": {
      "weatherCode": "cloudy",
      "weatherIcon": "⛅",
      "weatherName": "多云"
    },
    "totalPostsToday": 0,
    "hotTags": [
      { "name": "考试周", "count": 42 },
      { "name": "ddl", "count": 38 }
    ],
    "areas": [
      {
        "id": 1,
        "name": "图书馆",
        "code": "library",
        "lat": 30.6182,
        "lng": 114.2566,
        "weatherCode": "cloudy",
        "weatherIcon": "⛅",
        "weatherName": "多云",
        "postCount": 0,
        "dominantEmotion": "平静",
        "topTags": [],
        "emotionDistribution": {}
      }
    ]
  }
}
```

---

### 6.2 区域坐标列表

```
GET /api/v1/map/areas
```

无需认证。

**成功响应**

```json
{
  "code": 0,
  "data": {
    "center": { "lat": 30.6188, "lng": 114.2585 },
    "areas": [
      {
        "id": 1,
        "name": "图书馆",
        "code": "library",
        "lat": 30.6182,
        "lng": 114.2566,
        "boundary": [
          { "lat": 30.6177, "lng": 114.2561 },
          { "lat": 30.6187, "lng": 114.2561 },
          { "lat": 30.6187, "lng": 114.2571 },
          { "lat": 30.6177, "lng": 114.2571 }
        ]
      }
    ]
  }
}
```

---

## 7. 个人气象空间接口

### 7.1 今日个人天气

```
GET /api/v1/my-weather/today
Authorization: Bearer <token>
```

**成功响应**

```json
{
  "code": 0,
  "data": {
    "date": "2026-06-01",
    "weatherCode": "cloudy",
    "weatherIcon": "⛅",
    "weatherName": "多云",
    "summary": "今天还没有记录心情，去个人地图点击区域写点什么吧~",
    "tags": [],
    "postCount": 0
  }
}
```

---

### 7.2 个人地图数据

```
GET /api/v1/my-weather/map
Authorization: Bearer <token>
```

**成功响应**

```json
{
  "code": 0,
  "data": {
    "areas": [
      {
        "name": "图书馆",
        "code": "library",
        "weatherIcon": "⛅",
        "myPostCount": 0,
        "lat": 30.6182,
        "lng": 114.2566
      }
    ]
  }
}
```

---

### 7.3 情绪趋势

```
GET /api/v1/my-weather/trend?period=week
Authorization: Bearer <token>
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|:--:|------|
| period | string | week | `week`（最近7天）或 `month`（最近30天） |

**成功响应**

```json
{
  "code": 0,
  "data": [
    { "date": "2026-05-26", "weatherIcon": null },
    { "date": "2026-05-27", "weatherIcon": null }
  ]
}
```

---

### 7.4 气象日历

```
GET /api/v1/my-weather/calendar?month=2026-06
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| month | string | 是 | 月份，格式 `yyyy-MM` |

**成功响应**

```json
{
  "code": 0,
  "data": {
    "month": "2026-06",
    "days": [
      { "date": "2026-06-01", "weatherIcon": null }
    ],
    "stats": {
      "sunnyDays": 0,
      "cloudyDays": 0,
      "rainyDays": 0,
      "activeDays": 0
    }
  }
}
```

---

### 7.5 区域投稿分布

```
GET /api/v1/my-weather/distribution
Authorization: Bearer <token>
```

**成功响应**

```json
{
  "code": 0,
  "data": {
    "图书馆": 0,
    "教学楼": 0,
    "食堂": 0,
    "宿舍区": 0,
    "操场": 0,
    "篮球场": 0
  }
}
```

---

## 8. 校园区域编码

| code | 名称 | 坐标 (lat, lng) |
|------|------|-----------------|
| library | 图书馆 | 30.6182, 114.2566 |
| classroom | 教学楼 | 30.6195, 114.2580 |
| canteen | 食堂 | 30.6178, 114.2595 |
| dormitory | 宿舍区 | 30.6165, 114.2550 |
| playground | 操场 | 30.6200, 114.2605 |
| basketball | 篮球场 | 30.6210, 114.2615 |

---

## 9. 情绪 → 天气映射规则

| 情绪 | 天气编码 | 图标 | 天气名称 |
|------|----------|:--:|------|
| 开心 | sunny | ☀️ | 晴天 |
| 平静 | cloudy | ⛅ | 多云 |
| 压力 | overcast | ☁️ | 阴天 |
| 焦虑 | rainy | 🌧️ | 雨天 |
| 悲伤 | heavy_rain | ⛈️ | 暴雨 |
| 崩溃 | thunderstorm | 🌩️ | 雷暴 |

---

## 10. 接口汇总

| 方法 | 路径 | 认证 | 说明 |
|:---:|------|:--:|------|
| POST | `/api/v1/auth/register` | 否 | 注册 |
| POST | `/api/v1/auth/login` | 否 | 登录 |
| GET | `/api/v1/user/profile` | 是 | 个人资料 |
| POST | `/api/v1/posts/text` | 是 | 提交文字投稿 |
| GET | `/api/v1/posts/my` | 是 | 我的投稿历史 |
| DELETE | `/api/v1/posts/{postId}` | 是 | 删除投稿 |
| GET | `/api/v1/map/overview` | 否 | 全区域天气总览 |
| GET | `/api/v1/map/areas` | 否 | 区域坐标列表 |
| GET | `/api/v1/my-weather/today` | 是 | 今日个人天气 |
| GET | `/api/v1/my-weather/map` | 是 | 个人地图数据 |
| GET | `/api/v1/my-weather/trend` | 是 | 情绪趋势 |
| GET | `/api/v1/my-weather/calendar` | 是 | 气象日历 |
| GET | `/api/v1/my-weather/distribution` | 是 | 区域投稿分布 |

---

## 11. 认证说明

- token 通过注册/登录接口获取，位于响应 `data.token`
- 需要认证的接口在请求头携带 `Authorization: Bearer <token>`
- token 有效期 24 小时
- token 格式：`mock-token-{Base64(userId:expireTime)}`（后续替换为标准 JWT）
- 过期返回 `code=2004`，非法返回 `code=2005`
- 未携带 token 返回 `code=401`

---

## 12. 当前阶段说明

| 模块 | 状态 | 备注 |
|------|:--:|------|
| 用户认证 | 完整运行 | 注册/登录/JWT/鉴权 |
| 情绪分析 | mock | 关键词匹配，替换为 DeepSeek API 后生效 |
| 数据存储 | mock 内存 | MockDataStore，替换为 MySQL + MyBatis 后持久化 |
| 敏感词检测 | mock | 永远返回 false |
