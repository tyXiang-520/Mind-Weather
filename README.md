# MindWeather 心晴

南京大学鼓楼校区情绪气象站 —— 你说的话，变成校园上空的一片云。

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
npx vite --host 0.0.0.0
# 前端运行在 http://localhost:5173
```

浏览器打开 `http://localhost:5173`

## DeepSeek AI 配置（可选）

不配也能用，系统会 fallback 到本地关键词分析。配置后情绪分析更精准：

```bash
# 环境变量方式
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

或编辑 `user/src/main/resources/application.yaml`：

```yaml
mindweather:
  ai:
    api-key: sk-xxxxxxxxxxxxxxxx
```

## 修改 3D 模型

1. Blender 打开 `NJUmap.blend` 编辑
2. 导出 → glTF 2.0 (.glb) → 覆盖根目录 `NJUmap.glb`
3. 运行压缩：`node compress_glb.js`
4. 复制：`cp NJUmap_compressed.glb user/frontend/public/models/NJUmap.glb`
5. 刷新浏览器

建筑命名规则：`建筑名_部件名`（如 `北大楼_窗户_00`），部件会自动合并到主建筑。

新增/改名建筑后，需同步更新：
- 前端：`user/frontend/src/three/ZoneData.js`
- 后端：`user/src/main/java/.../business/config/ZoneConfig.java`

## 项目结构

```
user/
├── frontend/          # Vue 3 + Three.js 前端
│   └── src/
│       ├── views/     # HomePage / PostPage / MyPage
│       ├── components/# NJU3D (3D校园) / Map2D (2D个人地图)
│       ├── three/     # WeatherSystem / BuildingRegistry / ZoneData
│       └── api/       # 后端 API 调用
├── src/main/java/     # Spring Boot 后端
│   └── com/mindweather/user/
│       ├── entity/    # User / Post (JPA)
│       ├── repository/# JPA Repository
│       ├── service/   # 业务逻辑
│       ├── business/  # 情绪分析 / 天气聚合 / 分区配置
│       └── security/  # JWT 认证
└── docs/              # 需求文档
```

## 数据库

使用 H2 内嵌数据库，文件存储在 `user/data/mindweather.mv.db`，无需安装。

控制台：`http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/mindweather`
- 用户名: `sa`，密码留空
