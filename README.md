# MindWeather 心晴

南京大学鼓楼校区情绪气象站 —— 你说的话，变成校园上空的一片云。

## 功能概述

- **🌐 3D 校园** —— Blender 手工建模 + glTF 2.0 + Draco 压缩 + Three.js 渲染的鼓楼校区三维场景，根据全域情绪实时渲染天气效果（晴/多云/阴/雨/暴雨/雷暴），支持上帝视角与第一人称漫游切换
- **✍️ 心情投稿** —— 选择校园建筑，写下此刻感受，AI 自动分析情绪并映射为天气
- **🗺️ 天气总览** —— 12 个分区（A-L）的天气热力图，一键查看各区域情绪分布
- **🌤️ 个人气象站** —— 今日天气卡片、2D 投稿建筑分布地图、情绪趋势图、天气日历
- **💬 社交互动** —— 评论、点赞、匿名投稿
- **🚶 第一人称漫游** —— WASD 移动 + 跳跃 + 重力 + 碰撞检测，在校园中自由探索
- **🔐 JWT 认证** —— 邮箱注册/登录，安全会话管理

## 情绪 → 天气映射

| 情绪 | 天气 | 图标 |
|------|------|------|
| 愉悦 / 开心 | 晴天 | ☀️ |
| 平静 | 多云 | ⛅ |
| 疲惫 / 压力 | 阴天 | ☁️ |
| 低落 / 悲伤 | 雨天 | 🌧️ |
| 焦虑 | 暴雨 | ⛈️ |
| 崩溃 / 愤怒 | 雷暴 | 🌩️ |

## 环境要求

- **Java 21+** (JDK)
- **Node.js 18+**

## 快速启动

```bash
# 1. 安装前端依赖
cd user/frontend
npm install

# 2. 启动后端（新终端）
cd user
./mvnw spring-boot:run
# 后端运行在 http://localhost:8080

# 3. 启动前端（新终端）
cd user/frontend
npm run dev
# 前端运行在 http://localhost:5173
```

浏览器打开 `http://localhost:5173`

## AI 情绪分析配置（可选）

不配置也能用，系统会 fallback 到 Pollinations AI（免费）或本地关键词分析。配置 DeepSeek 后分析更精准：

```bash
# 环境变量方式
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

或编辑 `user/src/main/resources/application.yaml`：

```yaml
mindweather:
  ai:
    deepseek:
      enabled: true
      api-key: ${DEEPSEEK_API_KEY}   # 直接填写或通过环境变量
    pollinations:
      enabled: true                   # 免费备选，限流较严
```

**优先级**：DeepSeek > Pollinations AI（免费）> 本地关键词

## 项目结构

```
user/
├── frontend/                          # Vue 3 + Three.js 前端
│   └── src/
│       ├── App.vue                    # 根组件（路由 + TabBar）
│       ├── main.js                    # 入口
│       ├── views/                     # 页面视图
│       │   ├── HomePage.vue           # 首页（3D 校园场景）
│       │   ├── PostPage.vue           # 心情投稿页
│       │   ├── MyPage.vue             # 个人气象空间
│       │   ├── Building.vue           # 建筑详情页（该建筑下的投稿流）
│       │   ├── Profile.vue            # 个人资料设置
│       │   ├── Register.vue           # 注册页
│       │   └── DebugPage.vue          # 调试面板（/#/debug）
│       ├── components/                # UI 组件
│       │   ├── NJU3D.vue              # 3D 校园场景组件
│       │   ├── Map2D.vue              # 2D 个人投稿分布地图
│       │   ├── PostCard.vue           # 投稿卡片
│       │   ├── CommentList.vue        # 评论列表
│       │   ├── WeatherIcon.vue        # 天气图标
│       │   ├── WeatherOverviewPanel.vue # 天气总览面板
│       │   ├── ZonePostsPanel.vue     # 分区投稿面板
│       │   └── Toast.vue              # 提示组件
│       ├── three/                     # Three.js 3D 引擎
│       │   ├── WeatherSystem.js       # 天气粒子系统（雨/雪/雷电/云层）
│       │   ├── BuildingRegistry.js    # 建筑 → GLB mesh 注册与查找
│       │   ├── ZoneData.js            # 12 分区定义 & 建筑映射
│       │   ├── ViewControls.ts        # 第一人称漫游 + 上帝视角切换
│       │   └── INTEGRATION_GUIDE.md   # ViewControls 整合指南
│       ├── api/                       # 后端 API 调用
│       │   └── index.js               # 完整 REST 客户端
│       ├── utils/                     # 工具函数
│       │   ├── avatar.js              # 头像生成
│       │   └── debug.js               # 调试日志工具
│       ├── assets/
│       │   └── style.css              # 全局样式
│       └── __tests__/                 # 单元测试（Vitest）
│           ├── PostCard.test.js
│           ├── WeatherIcon.test.js
│           └── api.test.js
├── src/main/java/com/mindweather/user/ # Spring Boot 后端
│   ├── UserApplication.java           # 应用入口
│   ├── controller/                    # 认证 & 用户接口
│   │   ├── AuthController.java        # 注册 / 登录 / 个人资料
│   │   ├── UserController.java        # 用户信息
│   │   └── HealthController.java      # 健康检查
│   ├── business/                      # 核心业务模块
│   │   ├── controller/
│   │   │   ├── MoodController.java    # 投稿 CRUD
│   │   │   ├── CommentController.java # 评论功能
│   │   │   ├── LikeController.java    # 点赞功能
│   │   │   ├── MapController.java     # 3D 地图数据
│   │   │   ├── StatsController.java   # 统计接口
│   │   │   └── UserSpaceController.java # 个人气象空间
│   │   ├── service/                   # 业务接口
│   │   ├── service/impl/              # 业务实现
│   │   │   ├── MoodServiceImpl.java
│   │   │   ├── CommentServiceImpl.java
│   │   │   ├── LikeServiceImpl.java
│   │   │   ├── MapDisplayServiceImpl.java
│   │   │   ├── StatsServiceImpl.java
│   │   │   ├── UserSpaceServiceImpl.java
│   │   │   ├── DeepSeekEmotionService.java  # AI 情绪分析（多 provider）
│   │   │   ├── WeatherMappingServiceImpl.java # 情绪→天气映射
│   │   │   └── AreaWeatherServiceImpl.java   # 区域天气聚合
│   │   ├── dto/                       # 数据传输对象
│   │   ├── enums/
│   │   │   └── WeatherTypeEnum.java   # 天气类型（6 种）& 情绪映射表
│   │   └── config/
│   │       └── ZoneConfig.java        # 12 分区后端配置
│   ├── entity/                        # JPA 实体
│   │   ├── User.java
│   │   ├── Post.java
│   │   ├── Comment.java
│   │   ├── Like.java
│   │   └── MoodDailyStats.java        # 每日情绪统计
│   ├── repository/                    # JPA Repository
│   ├── security/                      # JWT 认证
│   │   ├── JWTManager.java            # Token 签发/验证
│   │   ├── JwtFilter.java             # 请求拦截
│   │   └── JwtErrorWriter.java        # 认证失败处理
│   ├── config/
│   │   └── SecurityConfig.java        # Spring Security 配置
│   ├── common/                        # 通用模块
│   │   ├── Result.java                # 统一响应体
│   │   ├── ErrorCode.java             # 错误码枚举
│   │   ├── BusinessException.java     # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   ├── dto/                           # 认证 DTO
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── UserInfoResponse.java
│   └── utils/
│       └── NicknameGenerator.java     # 随机昵称生成
└── docs/                              # 需求文档
```

## 数据库

### 开发环境：H2（默认）

使用 H2 内嵌数据库，文件存储在 `user/data/mindweather.mv.db`，无需安装外部数据库，开箱即用。

控制台：`http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/mindweather`
- 用户名: `sa`，密码留空

### 生产环境：MySQL

项目已内置 MySQL 驱动，切换只需修改 `user/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mindweather?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
```

> 切换 MySQL 后记得注释掉 H2 相关配置和依赖。

## API 概览

| 路径 | 说明 | 认证 |
|------|------|------|
| `POST /api/v1/auth/register` | 注册 | - |
| `POST /api/v1/auth/login` | 登录 | - |
| `GET/PUT /api/v1/auth/profile` | 查看/修改个人资料 | JWT |
| `PUT /api/v1/auth/password` | 修改密码 | JWT |
| `POST /api/v1/posts/text` | 发表心情投稿 | JWT |
| `GET /api/v1/posts/my` | 我的投稿列表 | JWT |
| `GET /api/v1/posts/building` | 建筑下投稿 | - |
| `GET /api/v1/posts/zone` | 分区下投稿 | - |
| `DELETE /api/v1/posts/{id}` | 删除投稿 | JWT |
| `GET/POST /api/v1/posts/{id}/comments` | 评论列表 / 发表评论 | 部分 JWT |
| `DELETE /api/v1/comments/{id}` | 删除评论 | JWT |
| `POST /api/v1/posts/{id}/like` | 点赞/取消 | JWT |
| `GET /api/v1/map/overview` | 3D 地图天气总览 | - |
| `GET /api/v1/map/areas` | 分区列表 | - |
| `GET /api/v1/my-weather/today` | 个人今日天气 | JWT |
| `GET /api/v1/my-weather/map` | 个人投稿分布 | JWT |
| `GET /api/v1/my-weather/trend` | 情绪趋势 | JWT |
| `GET /api/v1/my-weather/calendar` | 天气日历 | JWT |
| `GET /api/v1/my-weather/distribution` | 区域分布统计 | JWT |
| `GET /api/v1/stats/weather` | 天气分布统计 | - |
| `GET /api/v1/stats/today` | 今日全局统计 | - |
| `GET /api/v1/stats/my` | 个人统计 | JWT |

## 运行测试

```bash
cd user/frontend
npm test
```

## 修改 3D 模型

1. Blender 打开 `NJUmap.blend` 编辑
2. 导出 → glTF 2.0 (.glb) → 覆盖根目录 `NJUmap.glb`
3. 运行压缩：`node compress_glb.js`
4. 复制：`cp NJUmap_compressed.glb user/frontend/public/models/NJUmap.glb`
5. 刷新浏览器

建筑命名规则：`建筑名_部件名`（如 `北大楼_窗户_00`），部件会自动合并到主建筑。

新增/改名建筑后，需同步更新：
- 前端：[ZoneData.js](user/frontend/src/three/ZoneData.js) —— 分区 & 建筑映射
- 后端：[ZoneConfig.java](user/src/main/java/com/mindweather/user/business/config/ZoneConfig.java) —— 分区配置

## 第一人称漫游（开发中）

ViewControls 提供上帝视角 ↔ 第一人称无缝切换，支持 WASD 移动 + 跳跃 + 重力 + 碰撞检测。整合指南详见 [INTEGRATION_GUIDE.md](user/frontend/src/three/INTEGRATION_GUIDE.md)。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端框架 | Vue 3 (Composition API) |
| 3D 建模 | Blender → glTF 2.0 (.glb) |
| 3D 压缩 | Draco mesh compression |
| 3D 渲染引擎 | Three.js (WebGL) |
| 3D 操控 | OrbitControls（上帝视角）+ ViewControls（第一人称） |
| 2D 地图 | Canvas 2D 自绘 |
| 天气特效 | 自研粒子系统（雨/雪/雷电/云层） |
| 构建工具 | Vite 6 |
| 测试框架 | Vitest + jsdom |
| 后端框架 | Spring Boot 3.5 |
| 数据库 | H2 (dev) / MySQL (prod) |
| ORM | JPA + Hibernate |
| 认证 | JWT (jjwt) |
| AI | DeepSeek API + Pollinations AI |
| 密码加密 | BCrypt |
