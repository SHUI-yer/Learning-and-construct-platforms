# light-out · 综合学习平台后端

Spring Boot 3 + Maven 多模块的在线学习平台后端。项目采用"**公共模块 + 业务模块 + 唯一启动模块**"的拆分方式，每个业务域是一个独立 Maven 模块，最终由 `lp-starter` 聚合启动。

> 完整的模块级需求见 [../docs/功能整合文档.md](../docs/功能整合文档.md)；
> 仓库整体说明与已实现范围见 [../README.md](../README.md)。

---

## 模块结构

```
light-out/
├── pom.xml              # 聚合父 POM：Spring Boot 3.5.14 parent、统一依赖版本
├── mvnw / mvnw.cmd      # Maven Wrapper
├── compose.yaml         # 本地中间件编排（MySQL / Redis / RabbitMQ）
├── 数据库light_out .sql   # 全量建表脚本（89 张表）
│
├── lp-common/           # 公共能力：统一响应、全局异常、JWT、登录拦截器、角色常量
├── lp-auth/             # 认证授权：登录、用户、角色、院系、班级
├── lp-course/           # 课程管理：课程、章节、资源、选课、审核、复制
├── lp-assessment/       # 任务考核：任务、题库、试卷、提交、批阅、成绩、申诉、防作弊
├── lp-learning/         # 学习体验：进度、笔记、AI 问答、MQ、统计（⚠️ 未接入启动链路）
└── lp-starter/          # 唯一启动入口：聚合以上模块 + CORS/拦截器配置
```

### 模块职责与依赖

| 模块 | 职责 | 关键类 / 接口 | 依赖 |
|------|------|--------------|------|
| `lp-common` | 统一返回结构、异常体系、JWT 工具、登录态传递 | `Result` / `PageResult`、`GlobalExceptionHandler`、`JwtUtil`、`LoginInterceptor`、`UserContext`、`RoleConstant` | web、lombok、java-jwt |
| `lp-auth` | 登录认证与用户/角色数据 | `AuthController`（`POST /login`）、`UserService`、3 个注解式 Mapper | lp-common、mybatis |
| `lp-course` | 课程与章节资源 | `CourseController`（`/course/**`）、课程/章节/资源 Service、8 个 Mapper + 8 个 XML | lp-common、mybatis |
| `lp-assessment` | 任务与考核全流程 | 任务 `/task/**`、题目 `/question/**`、试卷 `/paper/**`、提交 `/submission/**`、成绩 `/gradeSummary/**`、申诉 `/gradeAppeal/**`、防作弊 `/cheatLog/**` | lp-common、mybatis、poi-ooxml |
| `lp-learning` | 学习进度、视频管控、笔记、AI 问答、MQ 消费、统计定时任务 | 9 个控制器（`/api/learning/**`）、Redis 与 RabbitMQ 配置、2 个消费者 | mybatis-plus、amqp、data-redis、knife4j |
| `lp-starter` | 唯一启动类与 Web 配置 | `LpStarterApplication`、`WebConfig`（CORS + 拦截器注册）、`application.yaml` | 上述业务模块 + web、data-redis、mybatis、mysql |

### Reactor 现状（重要）

父 POM 的 `<modules>` 中**只有 5 个模块处于激活状态**：

```
lp-common → lp-auth → lp-course → lp-assessment → lp-starter
```

- **`lp-learning` 被注释**：其 `pom.xml` 里的 `knife4j-openapi3-jakarta-spring-boot-starter` 缺少 `<version>`，会导致 reactor 校验失败。该模块的 70 个类（视频进度、学习统计、笔记、AI 问答、MQ 消费者）**既不在 reactor 中，也不在 `lp-starter` 的依赖里**，因此在运行中的应用里完全不可达。
- 另有 7 个模块目录尚未创建（`lp-peer-review`、`lp-community`、`lp-ai-engine`、`lp-self-study`、`lp-cet`、`lp-message`、`lp-admin`），已在父 POM 中注释占位。

---

## 技术栈

| 用途 | 选型 |
|------|------|
| 语言 / 框架 | Java 17、Spring Boot 3.5.14 |
| 构建 | Maven 多模块（父 POM 统一版本管理） |
| 持久层 | MyBatis 3.0.5（注解与 XML 混用） |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Lettuce 连接池） |
| 鉴权 | JWT（`com.auth0:java-jwt` 4.4.0）+ `LoginInterceptor` + `UserContext` |
| 消息队列 | RabbitMQ（仅 `lp-learning` 中使用，当前未接入） |
| 题库导入 | Apache POI 5.3.0（Excel / Word 解析） |

父 POM 的 `dependencyManagement` 已统一锁定：各内部模块版本、`spring-ai-bom` 1.1.7、MyBatis starter 3.0.5、`java-jwt` 4.4.0。**子模块不要重复写 `<version>`**。

---

## 构建与运行

### 前置条件

- JDK 17
- MySQL 8（库名 `light_out`）
- Redis（默认 `localhost:6379`，无密码）

### 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS light_out DEFAULT CHARSET utf8mb4;"
mysql -u root -p light_out < "数据库light_out .sql"
```

> 脚本文件名中 `数据库light_out` 与 `.sql` 之间**有一个空格**，引用时注意加引号。

### 2. 启动

> ⚠️ **启动前必须设置环境变量 `JWT_SECRET`**（至少 32 字节）。未设置时应用会在启动阶段直接失败，
> 这是刻意设计的 fail-fast：避免带着硬编码/弱密钥上线。

```bash
# 生成并设置密钥（每个环境一套，不要复用、不要入库）
# Windows PowerShell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
# Linux / macOS
export JWT_SECRET="$(openssl rand -base64 48)"
```

IDEA 用户可在 `Run/Debug Configurations → Environment variables` 中添加同名变量。

```bash
# 在 light-out 目录下
./mvnw -pl lp-starter -am spring-boot:run        # macOS / Linux
mvnw.cmd -pl lp-starter -am spring-boot:run      # Windows

# 或使用本机 Maven
mvn -pl lp-starter -am spring-boot:run
```

`-pl lp-starter -am` 表示"只构建 `lp-starter` 以及它所依赖的模块"，这是开发期最快的启动方式。服务默认监听 **8080** 端口。

### 3. 打包与运行

```bash
./mvnw -pl lp-starter -am -DskipTests package
java -jar lp-starter/target/lp-starter-0.0.1-SNAPSHOT.jar
```

> ⚠️ 只有 `lp-starter` 保留了 `spring-boot-maven-plugin`（可执行 jar）；其它模块打包出来的是普通 jar，不能单独启动。

### 4. 只构建 / 只测试某个模块

```bash
./mvnw -pl lp-course -am test          # 构建某模块及其依赖并跑测试
./mvnw clean                            # 清理所有模块的 target/
```

---

## 配置说明

### 生效的配置文件

| 文件 | 作用 |
|------|------|
| `lp-starter/src/main/resources/application.yaml` | **唯一实际生效**的运行配置（端口、数据源、Redis、MyBatis、日志） |
| `lp-common` / `lp-auth` / `lp-course` 的 `application.properties` | 仅声明各自 `spring.application.name`，位于同一 classpath 根，实际生效者取决于 jar 加载顺序 |
| `lp-learning/src/main/resources/application.yml` | 该模块自身的配置，因模块未接入启动链路而不生效 |

### 关键配置项（`application.yaml`）

| 配置 | 当前值 | 说明 |
|------|--------|------|
| `server.port` | `8080` | 服务端口 |
| `jwt.secret` | `${JWT_SECRET:}` | **必填**，由环境变量注入；缺失或短于 32 字节时启动失败 |
| `jwt.expire-hours` | `12` | 令牌有效期（小时），允许范围 1~720 |
| `security.password.legacy-md5` | `migrate` | 存量 MD5 账号处理策略：`migrate` 自动迁移 / `reject` 要求重置 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/light_out?...` | 含 `createDatabaseIfNotExist=true` |
| `mybatis.mapper-locations` | `classpath:mapper/*.xml` | 只加载 `lp-course` 的 8 个 XML（`lp-auth`/`lp-assessment` 用注解） |
| `spring.data.redis` | `localhost:6379` / db 0 | Lettuce 连接池，max-active 8 |
| `logging.level.com.learning` | `DEBUG` | 开发期日志 |

### JWT 与口令策略

**令牌（JWT）**

- 签名密钥只从 `jwt.secret` 读取（即环境变量 `JWT_SECRET`），源码里不再有任何默认密钥。
- 使用 HS256 并强制校验 `iss`（`learning-platform`）；签名错误、过期、issuer 不符、缺少业务载荷一律拒绝。
- 密钥短于 32 字节直接让启动失败——改造前的密钥是源码里的 2 个字符，等价于把"签发任意身份令牌"的能力随源码分发。
- 换密钥会让所有已签发令牌立即失效，属于预期行为（用户需重新登录）。

**口令（BCrypt）**

- 新口令一律用 BCrypt（cost 10，自带随机盐）写入，密码比较走 `PasswordEncoder`。
- 存量账号的密码是**无盐 MD5**，由 `PasswordHashes` 识别；默认策略 `migrate`：登录校验通过后立即用 BCrypt 覆写旧哈希，旧 MD5 值不再保留。
- 需要"强制重置"语义时，把 `security.password.legacy-md5` 改为 `reject`：存量账号登录会被拒绝并提示联系管理员重置。
  **注意**：当前仓库还没有改密/重置接口，直接切 `reject` 会锁死所有尚未迁移的账号。
- 无法识别的哈希格式一律拒绝，不会被当成"通过"。

### ⚠️ 凭据处理

`application.yaml` 中**当前直接写着数据库账号与密码**。正确做法是让配置文件不含密码，由环境变量或本地 profile 注入（Spring Boot 环境变量优先级高于 `application.yaml`）：

```bash
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/light_out?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=your_user
set SPRING_DATASOURCE_PASSWORD=your_password
```

或新建 `lp-starter/src/main/resources/application-local.yaml`（已被 `.gitignore` 忽略）：

```yaml
spring:
  datasource:
    username: your_user
    password: your_password
```

```bash
./mvnw -pl lp-starter -am spring-boot:run -Dspring-boot.run.profiles=local
```

### 关于 `compose.yaml`

`compose.yaml` 是 Spring Initializr 生成的模板，**不能直接与当前配置配套使用**：

- 服务名/凭据为 `mydatabase` / `myuser` / `secret`，与 `application.yaml` 的 `light_out` / `user` 不一致；
- 端口写成 `'3306'` 这种形式表示"映射到随机宿主机端口"，而配置里连的是固定 `localhost:3306`。

如要使用，请先改成固定端口映射并统一库名与账号：

```yaml
ports:
  - '3306:3306'
```

---

## 数据层约定

- **Mapper 位置**：接口在 `src/main/java/.../mapper/`，XML 在 `src/main/resources/mapper/`。
  - `lp-auth`、`lp-assessment`：注解式 Mapper（`@Select` / `@SelectProvider`），无 XML；
  - `lp-course`：8 个 XML；`lp-learning`：9 个 XML（未接入）。
- **MapperScan**：由启动类集中声明，新增模块必须同步添加包路径：

  ```java
  @MapperScan({"com.learning.lpauth.mapper",
               "com.learning.lpcourse.mapper",
               "com.learning.assessment.mapper"})
  ```

- **类型别名**：`type-aliases-package` 已被移除（`lpauth` 与 `lpcourse` 各有一个 `StudentClass` 实体，别名会冲突），所有 XML 使用全限定类名。
- **建表脚本**：`数据库light_out .sql`（89 张表，全量）。
- **命名**：数据库字段下划线 → 实体驼峰，依赖 `map-underscore-to-camel-case: true`。

---

## 接口约定

- **统一响应**：`Result<T>`，`code == 1` 表示成功；业务失败通常仍返回 HTTP 200，由前端按 `code` 判断。
- **鉴权**：`POST /login` 返回 JWT，后续请求带 `Authorization: Bearer <token>`；`LoginInterceptor` 拦截 `/**` 并放行 `/login`；当前用户信息通过 `UserContext` 在请求内传递。
- **角色**：`super_admin`(1) → `dept_admin`(2) → `teacher`(3) → `assistant`(4) → `student`(5)，数值越小权限越高，与前端 `src/constants/roles.ts` 一一对应。
- **跨域**：`WebConfig` 已开启全局 CORS（`allowedOriginPatterns("*")` + `allowCredentials(true)`），因此前端本地直连 8080 无需额外代理。

---

## 权限与数据范围

拦截器只回答"是否登录"，**"能做什么"由业务层判断**。考核模块统一走 `AssessmentAccessGuard`：

| 操作 | 允许角色 | 额外校验 |
|------|---------|---------|
| 开始作答 / 提交答卷 / 上报异常行为 | 学生 | 只能作用于自己的提交 |
| 批阅、设置优秀范本、查看班级提交与防作弊记录 | 管理员 / 教师 / 助教 | 必须是该任务所属课程的教师 |
| 任务、题库、试卷、导入记录的增删改 | 管理员 / 教师 / 助教 | 同上 |
| 成绩汇总 / 确认 / 导出 | 管理员 / 教师 / 助教 | 同上 |
| 申诉复核 | 管理员 / 教师 / 助教 | 同上 |
| 发起申诉、查看我的申诉 | 学生 | 只能针对自己的成绩 |

课程归属判定：`course.teacher_id`（创建者）或 `course_teacher` 关联表；`super_admin` / `dept_admin` 直接放行。

**敏感数据脱敏**（考试场景）：

- 试卷详情 `GET /paper/{id}`：学生只拿到题干与选项，`answer` / `analysis` 恒为 `null`；只有教师端返回参考答案。
- 答卷详情 `GET /submission/{id}`：学生只能看自己的；参考答案与解析仅在**已批阅**（`status=graded`）后返回。
- 提交列表 `GET /submission/list/{taskId}`：学生请求时服务端强制追加 `student_id = 当前用户`，不会把全班提交下发到浏览器。
- 成绩列表 `GET /gradeSummary/list/{courseId}`：学生只返回自己那条汇总，不再返回全班成绩与排名。

### 口令管理接口

| 接口 | 说明 | 权限 |
|------|------|------|
| `PUT /user/password` | 修改自己的密码，需提供 `oldPassword` | 已登录用户 |
| `POST /user/password/reset` | 管理员重置他人密码，无需旧密码（用于存量账号强制重置） | `super_admin` / `dept_admin` |

新口令统一走 `PasswordPolicy`：8~64 个字符、必须同时含字母和数字、不含空白、UTF-8 编码不超过 72 字节（BCrypt 上限）。

---

## 测试

| 模块 | 现状 |
|------|------|
| `lp-common` / `lp-auth` / `lp-course` / `lp-starter` | 各 1 个 Spring Initializr 遗留的 `contextLoads()`，无断言；`lp-starter` 的那个会真实装配 DataSource |
| `lp-common`（新增） | `JwtUtilTest`：弱密钥启动失败、其他密钥签发的令牌被拒、篡改/过期/缺 issuer 被拒、非法有效期被拒 |
| `lp-auth`（新增） | `PasswordHashesTest`：BCrypt 随机盐与校验、存量 MD5 识别与比对、MD5→BCrypt 迁移后行为一致、未知格式拒绝 |
| `lp-auth`（新增） | `PasswordPolicyTest`：长度上下限、必须含字母与数字、拒绝空白、拒绝超过 BCrypt 72 字节上限 |
| `lp-learning` | 6 个 Mockito 单元测试，因模块不在 reactor 中**从不被执行** |
| `lp-assessment` | 无测试 |

```bash
./mvnw -pl lp-starter -am test
```

---

## 新增模块清单

新增一个业务模块时，三处必须同时修改，否则代码不会进入运行中的应用：

1. `light-out/pom.xml` → `<modules>` 中添加模块；
2. `light-out/pom.xml` → `<dependencyManagement>` 中添加该模块的版本锁定；
3. `lp-starter/pom.xml` → 添加对该模块的依赖；
4. 若模块含 Mapper 接口，在 `LpStarterApplication` 的 `@MapperScan` 中追加包路径。

---

## 已知问题

| 问题 | 影响 | 建议 |
|------|------|------|
| `lp-learning` 未进入 reactor 与启动链路 | 学习进度、笔记、AI 问答、MQ、统计等功能不可用 | 换 `mybatis-plus-spring-boot3-starter`、补 `knife4j` 版本、补 `notes`/`subtitle`/`video_quality` 三张表，再取消父 POM 注释 |
| `application.yaml` 硬编码数据库口令 | 安全风险；换环境需改代码 | 改为环境变量 / 本地 profile 注入 |
| 明文凭据散落在 `项目结构.md` | 推到公开仓库即泄露 | 轮换并外置，见[仓库安全须知](../README.md#安全须知) |
| `lp-course` 存在第二个 `@SpringBootApplication`（`LpCourseApplication`） | 易误认为可独立启动，实际无打包插件与独立配置 | 删除该类，统一由 `LpStarterApplication` 启动 |
| `compose.yaml` 与 `application.yaml` 不匹配 | 无法直接一键起本地中间件 | 统一库名/账号，改固定端口映射 |
| 缺少改密 / 重置密码接口 | ~~存量账号只能靠默认 `migrate` 策略自动迁移~~ | **已修复**：新增 `PUT /user/password` 与 `POST /user/password/reset`，可支持 `reject` 策略 |
| 考试状态机未校验截止时间 | 交卷只看"限时试卷时长"，不校验 `deadline`；`allowLate` / `latePenalty` 落库后未被读取；`allowResubmit` 可无限重交并覆盖已批阅结果 | 在 start/submit 入口校验发布时间、截止时间与重交资格，并按比例扣分 |
| 申诉改分不回写 `submission.score` | 复核通过后教师端列表仍显示旧分，再次保存批阅会把申诉结果覆盖 | 统一改分入口，同时更新 `grade`、`grade_summary` 与 `submission.score` |
| 人工批阅不更新逐题分 | 只写整卷 `submission.score`，`exam_answer.score` 仍为自动判分结果，导致"总分 ≠ 逐题和" | 增加逐题打分接口，总分由逐题汇总 |
| 成绩口径 | 汇总只累加 `grade.weighted_score`，`grade_scheme` 配置的权重不参与计算 | 按 `grade_scheme` 分组件聚合后归一化，缺考计 0 |
| `mybatis.mapper-locations` 使用 `classpath:` 而非 `classpath*:` | 当前只有 `lp-course` 有 mapper XML 目录所以可用；接入 `lp-learning` 后其 XML 会全部静默不加载 | 改为 `classpath*:mapper/*.xml` 并按模块分目录 |

---

## 相关文档

- [../README.md](../README.md) — 仓库总览、已实现范围、快速开始
- [../docs/功能整合文档.md](../docs/功能整合文档.md) — 需求基线
- [项目结构.md](项目结构.md) — 后端模块规划与 Git 提交约定（⚠️ 含明文凭据）
