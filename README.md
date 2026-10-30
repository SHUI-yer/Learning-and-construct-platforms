# 综合学习平台 · Learning and Construction Platforms

> 一个面向高校场景的综合性学习平台：把**课程建设、任务考核、互评、学习进度、社区协作**装进同一条链路，并以 **AI 智能笔记引擎**作为核心差异化能力——不只是"视频仓库 + 成绩系统"，而是学生愿意主动打开的学习工具。

<p>
<img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white">
<img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white">
<img alt="Vite" src="https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white">
<img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white">
<img alt="Java" src="https://img.shields.io/badge/Java-17-E76F00?logo=openjdk&logoColor=white">
<img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
<img alt="Redis" src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white">
</p>

---

## 目录

- [项目简介](#项目简介)
- [仓库结构](#仓库结构)
- [功能规划](#功能规划)
- [当前实现状态](#当前实现状态)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [环境变量与配置](#环境变量与配置)
- [开发约定](#开发约定)
- [文档索引](#文档索引)
- [安全须知](#安全须知)
- [已知问题与路线图](#已知问题与路线图)

---

## 项目简介

大学的学习本质是自主化的。当下学习通、MOOC 等平台并不缺高质量资源，但真实观看人数有限——**资源本身已不是第一竞争力**。因此本平台定位为：

> 集教学管理、资源迭代、社区交流、自主学习、专项提升于一体的综合性学习建设平台，
> 同时兼顾教师的教学管理效率、学生的课堂学习需求与课外自主提升需求。

平台最核心的差异化能力是 **AI 智能笔记引擎**：把"找资源 → 看完 → 自己整理"的老路径，缩短为"丢链接 / 传文件 → AI 秒级解析 → 对话式深挖 → 一键生成笔记 / 导图 / 闪卡 / 测验"。AI 代替用户完成"阅读"，用户专注于"思考和提问"。

完整的模块级需求说明见 [docs/功能整合文档.md](docs/功能整合文档.md)。

---

## 仓库结构

本仓库是**前后端同仓**的多项目仓库，两个子工程各自独立构建、独立启动：

```
Learning and construct platforms/
├── learning-and-construction-platform_web/   # 前端：React + TypeScript + Vite
│   ├── src/
│   │   ├── api/          # 接口层，按业务域拆分（auth / course / assessment / user）
│   │   ├── components/   # 业务组件（assessment/ 下的题目卡片、作答输入、课程选择）
│   │   ├── constants/    # 枚举与常量（角色、课程、考核）
│   │   ├── hooks/        # 自定义 hooks（鉴权、课程、考核、倒计时、防作弊记录）
│   │   ├── layouts/      # MainLayout（主框架）/ AuthLayout（登录页骨架）
│   │   ├── pages/        # 页面，与路由一一对应
│   │   ├── router/       # 路由表与懒加载
│   │   ├── store/        # 全局状态（Zustand）
│   │   └── utils/        # axios 封装、权限判断、React Query 客户端
│   ├── public/           # 不参与编译的静态资源
│   ├── .env.example      # 环境变量模板
│   └── README.md         # 前端详细说明
│
├── light-out/                                # 后端：Maven 多模块 / Spring Boot 3
│   ├── pom.xml                    # 聚合父 POM，统一依赖版本
│   ├── lp-common/                 # 统一响应、全局异常、JWT、登录拦截器、角色常量
│   ├── lp-auth/                   # 认证授权（登录、用户、角色、院系、班级）
│   ├── lp-course/                 # 课程管理（课程、章节、资源、选课、审核、复制）
│   ├── lp-assessment/             # 任务考核（题库、试卷、提交、批阅、成绩、申诉、防作弊）
│   ├── lp-learning/               # 学习体验（进度、笔记、AI 问答、MQ、统计）※ 暂未接入启动链路
│   ├── lp-starter/                # 唯一启动入口，聚合各业务模块
│   ├── compose.yaml               # 本地中间件容器编排（MySQL / Redis / RabbitMQ）
│   ├── .env.example               # 后端与本地中间件变量模板
│   ├── 数据库light_out .sql        # 全量建表脚本（89 张表）
│   └── README.md                  # 后端详细说明
│
├── docs/                         # 项目文档
│   └── 功能整合文档.md             # 模块级功能需求（本项目的需求基线）
│
├── .gitignore                    # 仓库级忽略规则（前端 + 后端 + IDE + 敏感配置）
├── LICENSE                       # MIT
└── README.md                     # 本文件
```

---

## 功能规划

需求基线来自 [docs/功能整合文档.md](docs/功能整合文档.md)，共 13 个模块：

| 模块 | 核心内容 | 主要角色 |
|------|---------|---------|
| 用户与权限 | 超级管理员 → 分院管理员 → 任课教师 → 助教 → 学生，五级权限；批量导入账号 | 管理员 |
| 课程管理与建设 | 课程导入/自建、审核上线、章-节-小节三级目录、多模态内容、素材库复用、课程复制、选课 | 管理员 / 教师 / 学生 |
| 任务与考核 | 成绩构成定义、任务发布与待办、五类题型题库、Excel/Word 批量导入、组卷、批阅、成绩汇总与申诉、防作弊 | 教师 / 学生 |
| 互评系统 | 系统随机分配 3–5 份作业、评分量表、全程匿名、结果申诉、优秀范本流通 | 学生 / 教师 |
| 学习体验与进度 | 断点续学、倍速/字幕/清晰度、时间轴笔记、即时问答、学习报告；教师侧进度跟踪、防拖拽、数据大屏 | 学生 / 教师 |
| 消息、通知与待办 | 课程群聊、私信、通知推送、系统公告、待办清单 | 全部用户 |
| 社区交流与资源共享 | 分区讨论、教学资源分享、学生资源拓展、AI 笔记分享、社区维护 | 全部用户 |
| 自主学习 | 跨学科选课、四种学习模式适配、学习报告、错题本、AI 笔记本 | 学生 |
| 四六级专项 | 词书与艾宾浩斯计划、闯关练习、听读写译专项、模拟考试、词库维护 | 学生 |
| AI 智能笔记引擎 | 多源接入（视频/网页/PDF/DOI/音频）、结构化摘要、时间轴索引、术语表、FAQ、思维导图、闪卡、音频概述、引用式对话、笔记协作、知识图谱 | 学生 / 教师 |
| 创新功能 | 节点式编程学习、开源式知识库（Wiki-Syllabus）、难点悬赏榜单 | 全部用户 |
| 系统管理 | 模块管理、数据大屏、系统公告、社区维护、AI 引擎参数与配额 | 管理员 |

> 详细到子模块与角色的完整说明见 [docs/功能整合文档.md](docs/功能整合文档.md)。

---

## 当前实现状态

> ⚠️ **本仓库目前处于骨架阶段**：需求文档共规划 13 个模块，当前代码聚焦在"登录 + 课程管理 + 任务考核"三条主线，其余模块尚在规划中。下表以**代码实际存在**为准，不以文档为准。

| 模块 | 前端 | 后端 | 说明 |
|------|:----:|:----:|------|
| 登录与鉴权 | ✅ | ✅ | `POST /login`，JWT + 本地存储，401/403 统一跳登录页 |
| 五级角色模型 | ✅ | ✅ | `super_admin / dept_admin / teacher / assistant / student`，前后端常量一一对应 |
| 课程管理与建设 | ✅ | ✅ | 课程增改查、导入、审核、复制、章节树与排序、资源增删改 |
| 学生选课视图 | 🚧 | ✅ | 「我的课程」列表可用；课程详情页受权限前缀匹配缺陷影响，学生点击会被弹回首页（见[已知问题](#已知问题)） |
| 任务与考核（教师端） | ✅ | ✅ | 任务发布、题库、组卷/自动组卷、提交批阅、成绩汇总、申诉复核、防作弊记录 |
| 考试（学生端） | ✅ | ✅ | 开始考试、限时作答、提交、成绩与申诉，含倒计时与切屏记录 |
| 个人中心 | 🚧 | 🚧 | 页面已存在，`src/api/user.ts` 中的用户信息接口仍是注释状态 |
| 学习体验与进度 | 📋 | 🚧 | 后端 `lp-learning` 已有 70 个类，但**未列入 reactor 且未接入启动链路**，运行中的服务访问不到 |
| AI 智能笔记引擎 | 📋 | 📋 | 尚未实现；`lp-learning` 中的 AI 问答为固定返回的桩实现 |
| 互评 / 社区 / 消息 / 四六级 / 自主学习 / 系统管理 / 创新功能 | 📋 | 📋 | 仅存在于需求文档，尚无对应目录 |

图例：✅ 已实现可用 · 🚧 部分实现 · 📋 规划中

---

## 技术栈

### 前端（`learning-and-construction-platform_web`）

| 用途 | 选型 |
|------|------|
| 框架 | React 19 + TypeScript 6 |
| 构建 | Vite 8（`@vitejs/plugin-react` + React Compiler + Tailwind CSS 4） |
| UI 组件库 | Ant Design 6 + `@ant-design/icons` |
| 路由 | React Router 7（页面全部 `lazy` 懒加载） |
| 状态管理 | Zustand 5（按业务域切片） |
| 服务端状态 | TanStack React Query 5 |
| 请求 | Axios（统一 `Result<T>` 解包 + 拦截器） |
| 样式 | Tailwind CSS 4 + Ant Design 主题 |
| 校验与规范 | ESLint 10 + typescript-eslint |

### 后端（`light-out`）

| 用途 | 选型 |
|------|------|
| 语言与框架 | Java 17 + Spring Boot 3.5.14 |
| 构建 | Maven 多模块（父 POM 统一版本管理） |
| 持久层 | MyBatis 3.0.5（注解 + XML 混用，XML 位于各模块 `resources/mapper/`） |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Lettuce 连接池） |
| 鉴权 | JWT（`com.auth0:java-jwt`）+ 自定义 `LoginInterceptor` + `UserContext` |
| 消息队列 | RabbitMQ（`lp-learning` 中已配置，当前未接入启动链路） |

---

## 快速开始

### 0. 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Node.js | ≥ 20.19（推荐 22 LTS） | Vite 8 的最低要求 |
| JDK | 17 | 后端 `java.version=17` |
| Maven | ≥ 3.9 | 也可使用各模块自带 `mvnw` |
| MySQL | 8.x | 库名 `light_out` |
| Redis | 6.x / 7.x | 默认 `localhost:6379`，无密码 |

### 1. 初始化数据库

```bash
# 建库 + 导入全量表结构（89 张表）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS light_out DEFAULT CHARSET utf8mb4;"
mysql -u root -p light_out < "light-out/数据库light_out .sql"
```

### 2. 启动后端（端口 8080）

> ⚠️ **必须先设置环境变量 `JWT_SECRET`**（至少 32 字节）。后端启动时读取它作为 JWT 签名密钥，
> 未设置会直接启动失败——这是刻意的 fail-fast，避免再次出现硬编码弱密钥。

```bash
# Windows PowerShell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
# Linux / macOS
export JWT_SECRET="$(openssl rand -base64 48)"
```

```bash
# 在仓库根目录执行；-pl lp-starter -am 会连带构建其依赖模块
mvn -f light-out/pom.xml -pl lp-starter -am spring-boot:run
```

也可以直接运行打包产物：

```bash
mvn -f light-out/pom.xml -pl lp-starter -am -DskipTests package
java -jar light-out/lp-starter/target/lp-starter-0.0.1-SNAPSHOT.jar
```

启动前请先确认 MySQL / Redis 可用，并按需通过环境变量覆盖连接信息（见下一节）。

### 3. 启动前端（端口 5173）

```bash
cd learning-and-construction-platform_web
npm install

# 复制环境变量模板（.env 不入库）
cp .env.example .env      # Windows: copy .env.example .env

npm run dev               # http://localhost:5173
```

其他常用命令：

```bash
npm run build     # 类型检查 + 生产构建，产物在 dist/
npm run preview   # 预览构建产物
npm run lint      # ESLint 检查
```

### 4. 端口与连通性

| 服务 | 地址 |
|------|------|
| 前端开发服务器 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| MySQL | localhost:3306 / 库 `light_out` |
| Redis | localhost:6379 |

前端通过 `VITE_API_BASE_URL` 直连后端；后端 `WebConfig` 已放开全局 CORS（`allowedOriginPatterns("*")` + `allowCredentials`），因此本地跨端口调试无需额外配置代理。

---

## 环境变量与配置

### 前端

只有以 `VITE_` 开头的变量会被注入到浏览器端，**不要把任何密钥放进前端变量**。

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api` | 后端地址。本地开发填 `http://localhost:8080`；部署到网关/nginx 后可留空，走同源 `/api` |

`.env` 已被 `.gitignore` 忽略，仓库只保留 `.env.example`。

### 后端

**必填：`JWT_SECRET`**（JWT 签名密钥，至少 32 字节）。缺失或过短时后端无法启动，请按[启动后端](#2-启动后端端口-8080)中的命令生成。

`light-out/lp-starter/src/main/resources/application.yaml` 只保留非敏感默认值，数据库、Redis、RabbitMQ 密码和 JWT 密钥均通过环境变量注入。仓库中提供 `light-out/.env.example` 作为变量清单；真实的 `light-out/.env` 不入库：

```bash
# Spring Boot 进程启动前设置环境变量（优先级高于 application.yaml）
set JWT_SECRET=请替换为至少32字节的随机串
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/light_out?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=your_user
set SPRING_DATASOURCE_PASSWORD=your_password
set SPRING_DATA_REDIS_HOST=localhost
set SPRING_DATA_REDIS_PORT=6379
set SPRING_DATA_REDIS_PASSWORD=your_redis_password
mvn -f light-out/pom.xml -pl lp-starter -am spring-boot:run
```

```yaml
# 也可以复制模板为 application-local.yaml（该文件已被 .gitignore 忽略）
# 模板：light-out/lp-starter/src/main/resources/application-local.yaml.example
spring:
  datasource:
    username: your_user
    password: your_password
```

```bash
# 用本地 profile 启动（JWT_SECRET 仍通过环境变量提供）
mvn -f light-out/pom.xml -pl lp-starter -am spring-boot:run -Dspring-boot.run.profiles=local
```

其他常用配置项（均在 `application.yaml` 中）：

| 配置 | 当前值 | 说明 |
|------|--------|------|
| `server.port` | `8080` | 服务端口 |
| `jwt.secret` | `${JWT_SECRET:}` | **必填**，环境变量注入 |
| `jwt.expire-hours` | `12` | 令牌有效期（小时） |
| `security.password.legacy-md5` | `migrate` | 存量 MD5 账号：`migrate` 登录后自动升级为 BCrypt，`reject` 要求重置 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/light_out` | 库名 `light_out` |
| `spring.datasource.password` | `${SPRING_DATASOURCE_PASSWORD:}` | 通过环境变量注入 |
| `spring.data.redis` | `localhost:6379`，db 0 | |
| `spring.data.redis.password` | `${SPRING_DATA_REDIS_PASSWORD:}` | 通过环境变量注入 |
| `mybatis.mapper-locations` | `classpath:mapper/*.xml` | |
| `logging.level.com.learning` | `DEBUG` | |

---

## 开发约定

### 分支与提交

- **主干开发 + 功能分支**：新功能在独立分支开发，自测通过后再合并回 `main`。
- **尽量避免强制推送**（`git push -f`），保护他人已拉取的历史。

### 前端

- 接口函数统一写在 `src/api/` 下，按业务域分文件；**页面层不直接调用 `axios`**，统一走 `src/utils/request.ts` 的 `http` 包装。
- 组件 props 必须有 TypeScript 类型定义；新增页面放在 `src/pages/` 下按业务域建子目录，并导出默认组件。
- 后端统一返回 `Result<T>`（`code === 1` 表示成功），拦截器已解包 `data`，接口层拿到的直接是 `T`；业务失败请抛 `ApiError`。
- 角色判断统一使用 `src/constants/roles.ts` + `src/utils/permission.ts`，不要硬编码角色字符串。

### 后端

- 新增业务能力时，按"模块"落地：实体 / DTO / VO / mapper / service(impl) / controller 分层，模块内自洽。
- 新增模块需要同时修改三处，否则代码不会进入运行中的应用：
  1. `light-out/pom.xml` 的 `<modules>`；
  2. 启动模块 `lp-starter/pom.xml` 的依赖；
  3. `LpStarterApplication` 的 `@MapperScan`（如该模块使用 Mapper 接口）。
- 内部模块版本统一由父 POM 的 `dependencyManagement` 管理，子模块不要重复写 `<version>`。

---

## 文档索引

| 文档 | 内容 |
|------|------|
| [docs/功能整合文档.md](docs/功能整合文档.md) | **需求基线**：13 个模块的功能说明、角色分工、核心业务流程 |
| [learning-and-construction-platform_web/README.md](learning-and-construction-platform_web/README.md) | 前端：目录结构、路由表、接口层与状态管理约定 |
| [learning-and-construction-platform_web/README.en.md](learning-and-construction-platform_web/README.en.md) | 前端说明（英文版） |
| [learning-and-construction-platform_web/功能分工.md](learning-and-construction-platform_web/功能分工.md) | 前端团队分工与三阶段开发计划 |
| [light-out/README.md](light-out/README.md) | 后端：模块职责、构建启动、配置与测试说明 |
| [light-out/项目结构.md](light-out/项目结构.md) | 后端多模块规划、Git 提交约定、中间件地址（⚠️ 含明文凭据，见下节） |

---

## 安全须知

> ⚠️ **任何真实凭据都不得进入 Git。** 本项目的生产凭据只保存在服务器环境文件中。

1. 生产凭据已轮换，并已从当前配置和项目文档移出。
2. 真实值通过环境变量或服务器私有环境文件注入，仓库只保留 `.env.example` 和配置说明模板。
3. 提交前运行 `pwsh ./scripts/check-sensitive.ps1`，并确认 `git status --ignored` 中本地 `.env`、构建产物和备份文件均未被跟踪。
4. 旧凭据仍可能存在于 Git 历史；如远程仓库曾经可被非授权人员访问，应在团队确认后使用 `git filter-repo` 等工具清理历史，并协调一次强制推送。

### 已完成的加固

- **JWT 密钥已外置**：`JwtUtil` 不再包含任何默认密钥，只从 `jwt.secret`（环境变量 `JWT_SECRET`）读取；短于 32 字节直接启动失败。令牌强制校验 `iss`，签名错误/过期/篡改一律拒绝。
- **口令哈希已升级为 BCrypt**：新口令一律 BCrypt（随机盐，cost 10）；存量无盐 MD5 账号在下次登录校验通过后立即被覆写为 BCrypt，旧哈希不保留。需要"强制重置"语义时把 `security.password.legacy-md5` 设为 `reject`。
- **考核模块已补授权与脱敏**：新增统一的 `AssessmentAccessGuard`（角色 + 课程归属校验）；试卷详情、答卷详情不再向学生下发参考答案与解析；班级提交列表与成绩列表对学生收敛为"只看自己的记录"。
- **口令管理已闭环**：新增 `PUT /user/password`（自助改密，需校验旧密码）与 `POST /user/password/reset`（管理员重置），这样 `legacy-md5=reject` 才有可用的重置路径。
- **注意**：切换密钥会让所有已签发令牌失效；存量账号首次登录后其密码哈希即被迁移，因此**请不要在迁移期间回滚到旧版本**（旧代码读不懂 BCrypt 哈希）。

---

## 已知问题与路线图

### 已知问题

| 问题 | 影响 | 建议 |
|------|------|------|
| 菜单项指向不存在的路由 | `src/utils/permission.ts` 的 `MENU_CONFIG` 中有 `/learning`、`/tools/notebook`、`/tools/mindmap`、`/admin/users`、`/admin/roles`，但 `router.tsx` 未定义这些路由，点击后落入 404 兜底页 | 为未完成的菜单项加"敬请期待"占位页，或先移除对应菜单 |
| 学生无法打开课程详情 | `/courses` 仅对教师/管理员开放，而 `canAccessPath` 用前缀匹配，`/courses/:id` 也被一并拒绝，学生在「我的课程」点卡片会被重定向回首页 | 在 `ROUTE_PERMISSIONS` 中为 `/courses/:id` 显式放行学生，或改为精确匹配 |
| `lp-learning` 被排除在 reactor 之外 | 学习进度、笔记、AI 问答、MQ 消费、定时统计全部不在启动链路上，运行中的服务无法访问这些功能 | 补上 `lp-learning/pom.xml` 中缺失版本的 `knife4j` 依赖，取消 `light-out/pom.xml` 中的注释，并把模块加入 `lp-starter` 依赖与 `@MapperScan` |
| 需求文档列出的 7 个模块目录不存在 | `lp-peer-review`、`lp-community`、`lp-ai-engine`、`lp-self-study`、`lp-cet`、`lp-message`、`lp-admin` 尚无代码 | 按迭代计划逐个落地，落地时同步更新本 README 的状态表 |
| 后端配置文件硬编码数据库口令 | 安全风险 + 换环境需改代码 | 改为环境变量 / 本地 profile |
| `application.yaml` 中文注释乱码 | 维护体验差 | 统一用 UTF-8 保存该文件 |
| 根目录残留 `package.json` / `package-lock.json` / `node_modules` | 仓库根目录并非 npm 工程，易误导 | 确认无用后删除；相关依赖应在 `learning-and-construction-platform_web` 内管理 |
| 仓库内嵌套了同名远程仓库目录 `Learning-and-construct-platforms/` | 重复内容，易被误提交 | 已加入 `.gitignore`；确认后可直接删除该目录 |
| 后端测试基本为空 | 缺乏回归保障 | `lp-common` / `lp-auth` / `lp-course` / `lp-starter` 仅有空的 `contextLoads()`；`lp-assessment` 无测试 |

### 路线图

- [ ] 打通 `lp-learning`，上线断点续学、视频管控与学情统计
- [ ] 实现互评系统（随机分配、匿名评分、申诉）
- [ ] 落地 AI 智能笔记引擎（多源接入 → 结构化笔记 → 引用式问答）
- [ ] 消息通知与待办中心
- [ ] 社区与资源分享
- [ ] 四六级专项模块
- [ ] 管理后台与数据大屏
- [ ] 补齐后端单元测试与接口测试

---

## License

[MIT](LICENSE) © 2026 SHUIYE
