# 智学汇平台 · 前端（learning-and-construction-platform_web）

综合学习平台的前端工程：基于 **React 19 + TypeScript + Vite 8 + Ant Design 6** 构建，当前承载「课程管理」与「任务考核（含在线考试、防作弊）」两条业务主线。

> 仓库总览与整体架构见 [../README.md](../README.md)；
> 需求基线见 [../docs/功能整合文档.md](../docs/功能整合文档.md)。

---

## 目录

- [当前实现范围](#当前实现范围)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [环境变量](#环境变量)
- [路由表](#路由表)
- [角色与权限](#角色与权限)
- [接口层约定](#接口层约定)
- [状态管理](#状态管理)
- [自定义 Hooks](#自定义-hooks)
- [页面与组件](#页面与组件)
- [开发约定](#开发约定)
- [已知问题](#已知问题)

---

## 当前实现范围

| 能力 | 状态 | 说明 |
|------|:----:|------|
| 登录鉴权 | ✅ | 账号密码登录，JWT 存 localStorage，过期/401 自动跳登录页 |
| 课程管理 | ✅ | 列表分页筛选、新建、编辑、审核、复制、章节树、资源增删 |
| 学生选课 | 🚧 | "我的课程"列表可用；课程详情页存在权限拦截缺陷（见[已知问题](#已知问题)） |
| 任务管理 | ✅ | 任务类型、发布方式、截止时间、成绩权重、迟交与重交策略 |
| 题库 | ✅ | 五种题型 CRUD、Excel/Word 批量导入与错误明细、导入记录 |
| 试卷 | ✅ | 手动选题 + 按题型/难度/数量/分值智能抽题、编辑、发布为任务 |
| 在线考试 | ✅ | 限时作答、倒计时自动交卷、本地草稿、防作弊记录 |
| 批阅与成绩 | ✅ | 打分评语、优秀范本、成绩构成权重、汇总确认、Excel 导出 |
| 成绩申诉 | ✅ | 学生申诉 → 教师复核（通过需改分、驳回需理由） |
| 个人中心 | 🚧 | 页面已存在，`src/api/user.ts` 接口仍为注释状态，暂为只读展示 |
| AI 笔记 / 视频播放 / 社区 / 四六级 / 管理后台 | 📋 | 仅存在于需求文档与菜单占位，尚无实现 |

图例：✅ 已实现 · 🚧 部分实现 · 📋 规划中

---

## 技术栈

| 用途 | 选型 | 版本 |
|------|------|------|
| 框架 | React | 19.2 |
| 语言 | TypeScript | 6.0 |
| 构建 | Vite | 8.0 |
| UI 组件库 | Ant Design + `@ant-design/icons` | 6.4 / 6.2 |
| 路由 | React Router | 7.16 |
| 状态管理 | Zustand | 5.0 |
| 服务端状态 | TanStack React Query | 5.102 |
| 请求 | Axios | 1.20 |
| 样式 | Tailwind CSS（`@tailwindcss/vite`）+ Ant Design 主题 | 4.3 |
| 时间处理 | dayjs | 1.11 |
| 代码规范 | ESLint（flat config）+ typescript-eslint | 10.4 / 8.59 |

构建链额外启用：

- **React Compiler**：`@vitejs/plugin-react` 的 `reactCompilerPreset` + `@rolldown/plugin-babel`（无需手写 `useMemo` / `useCallback`）。
- **Tailwind CSS 4**：`src/index.css` 中仅 `@import "tailwindcss";`，没有 `tailwind.config.js` / `postcss.config.js`。

> TypeScript 配置未开启 `strict`，且未配置 `@/` 路径别名，所有导入使用相对路径。

---

## 目录结构

```
src/
├── api/                     # 接口层：按业务域拆文件，页面不直接调 axios
│   ├── assessment.ts        # 任务、题库、试卷、提交、批阅、成绩、申诉、防作弊
│   ├── auth.ts              # 登录
│   ├── course.ts            # 课程、章节、资源
│   ├── types.ts             # Result / PageResult 及全部 VO、DTO 类型
│   └── user.ts              # 个人信息接口（目前全部为注释占位）
│
├── components/
│   └── assessment/          # 考核相关业务组件
│       ├── AnswerInput.tsx  # 按题型渲染作答控件（单选/多选/判断/填空/简答）
│       ├── CourseSelect.tsx # 课程下拉选择（学生模式只列已发布课程）
│       └── QuestionCard.tsx # 题干 + 选项 + 解析 + 批阅结果
│
├── constants/               # 枚举与常量
│   ├── assessment.ts        # 任务类型、发布方式、提交状态等
│   ├── course.ts            # 课程状态等
│   └── roles.ts             # 五级角色：编码、中文名、等级
│
├── hooks/                   # 业务 hooks（基于 React Query 封装）
│   ├── useAssessment.ts     # 任务/题库/试卷/提交/成绩/申诉/防作弊数据
│   ├── useAuth.ts           # 从 store 派生当前用户与角色判断
│   ├── useCheatRecorder.ts  # 切屏、失焦、复制粘贴等行为采集与上报
│   ├── useCountdown.ts      # 依据服务端截止时间倒计时
│   └── useCourses.ts        # 课程列表、课程详情、章节树
│
├── layouts/
│   ├── AuthLayout.tsx       # 登录页骨架
│   └── MainLayout.tsx       # 主框架：侧边菜单 + 顶栏 + 鉴权与权限守卫
│
├── pages/
│   ├── Home.tsx             # 首页欢迎页（含统计占位）
│   ├── Profile.tsx          # 个人中心（只读）
│   ├── login.tsx            # 登录页
│   ├── course/              # CourseList / CourseDetail / MyCourses
│   ├── assessment/          # 教师端：任务、题库、试卷、阅卷、成绩、申诉
│   └── myAssessment/        # 学生端：任务、考试、成绩、申诉
│
├── router/
│   ├── index.tsx            # 路由实例
│   └── router.tsx           # 路由表（页面全部 lazy 懒加载）
│
├── store/
│   └── authStore.ts         # Zustand：用户信息、登录/登出、localStorage 持久化
│
├── utils/
│   ├── permission.ts        # 菜单配置、角色过滤、路径权限判断
│   ├── queryClient.ts       # React Query 全局配置
│   └── request.ts           # Axios 实例、拦截器、Result 解包、文件下载
│
├── App.tsx / App.css        # 应用根组件（App.css 为脚手架残留）
├── main.tsx                 # 入口，挂载 Router 与 QueryClientProvider
└── index.css                # Tailwind 入口
```

---

## 快速开始

### 前置条件

- Node.js **≥ 20.19**（推荐 22 LTS，Vite 8 的最低要求）
- 后端服务已启动在 `http://localhost:8080`

### 安装与启动

```bash
cd learning-and-construction-platform_web
npm install

# 复制环境变量模板（.env 不入库）
cp .env.example .env        # Windows: copy .env.example .env

npm run dev                 # http://localhost:5173
```

### 全部脚本

| 命令 | 作用 |
|------|------|
| `npm run dev` | 启动开发服务器（Vite，默认 5173 端口） |
| `npm run build` | `tsc -b && vite build`，产物输出到 `dist/` |
| `npm run preview` | 本地预览 `dist/` 构建产物 |
| `npm run lint` | ESLint 检查 `**/*.{ts,tsx}` |

> 没有 test / format 脚本，当前也没有单元测试。

---

## 环境变量

只有以 `VITE_` 开头的变量会被注入浏览器端，**禁止放入任何密钥**。

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api` | 后端地址。本地开发为 `http://localhost:8080`；部署到网关/nginx 后可留空，走同源 `/api` |

**这是唯一的环境变量**。仓库只保留 `.env.example`，`.env` 已被 `.gitignore` 忽略。

### 关于跨域

`vite.config.ts` **没有配置 `server.proxy`**，前端是按 `VITE_API_BASE_URL` 直连后端的。之所以本地能跑通，是因为后端 `WebConfig` 放开了全局 CORS（`allowedOriginPatterns("*")` + `allowCredentials(true)`）。

若后端关闭了宽松 CORS，请在前端补一个开发代理，而不是把后端继续放开：

```ts
// vite.config.ts
export default defineConfig({
  server: {
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
})
```

---

## 路由表

| 路径 | 页面组件 | 布局 | 可访问角色 |
|------|---------|------|-----------|
| `/` | `pages/Home` | MainLayout | 全部已登录用户 |
| `/profile` | `pages/Profile` | MainLayout | 全部已登录用户 |
| `/courses` | `pages/course/CourseList` | MainLayout | 超级管理员 / 分院管理员 / 教师 / 助教 |
| `/courses/:id` | `pages/course/CourseDetail` | MainLayout | 同上（⚠️ 学生也会被拦截，见已知问题） |
| `/my-courses` | `pages/course/MyCourses` | MainLayout | 学生 |
| `/assessment/tasks` | `pages/assessment/TaskList` | MainLayout | 教师 / 管理员 / 助教 |
| `/assessment/questions` | `pages/assessment/QuestionBank` | MainLayout | 同上 |
| `/assessment/papers` | `pages/assessment/PaperManage` | MainLayout | 同上 |
| `/assessment/tasks/:taskId/submissions` | `pages/assessment/SubmissionReview` | MainLayout | 同上（无独立菜单，从任务列表进入） |
| `/assessment/grades` | `pages/assessment/GradeManage` | MainLayout | 同上 |
| `/assessment/appeals` | `pages/assessment/AppealReview` | MainLayout | 同上 |
| `/my-assessment/tasks` | `pages/myAssessment/TaskList` | MainLayout | 学生 |
| `/my-assessment/exam/:taskId` | `pages/myAssessment/ExamPage` | MainLayout | 学生 |
| `/my-assessment/grades` | `pages/myAssessment/Grades` | MainLayout | 学生 |
| `/my-assessment/appeals` | `pages/myAssessment/MyAppeals` | MainLayout | 学生 |
| `/login` | `pages/login` | AuthLayout | 未登录用户 |
| `*` | 内联 404 组件 | 无 | — |

页面全部通过 `React.lazy` 懒加载，`MainLayout` 内用 `<Suspense>` 兜底。

---

## 角色与权限

### 角色模型

`src/constants/roles.ts` 定义五级角色，`roleLevel` 数值越小权限越高，与后端 `RoleConstant.java` 一一对应：

| 编码 | 名称 | 等级 |
|------|------|:----:|
| `super_admin` | 超级管理员 | 1 |
| `dept_admin` | 分院管理员 | 2 |
| `teacher` | 任课教师 | 3 |
| `assistant` | 助教 | 4 |
| `student` | 学生 | 5 |

### 权限实现方式

项目**没有** `RequireAuth` 组件或路由 `meta.roles`，权限全部集中在两处：

1. `src/utils/permission.ts`
   - `MENU_CONFIG`：菜单树，每项可声明 `roles`（不填表示所有角色可见），同时作为**路径权限的来源**；
   - `ROUTE_PERMISSIONS`：为不在菜单中、但需要控制的路由补充规则（目前仅登记 `/my-assessment`）；
   - `filterMenuByRole()`：按角色剪枝菜单；
   - `canAccessPath()`：先查 `ROUTE_PERMISSIONS`，再在菜单树中做**精确匹配或前缀匹配**。
2. `src/layouts/MainLayout.tsx`
   - 未登录 → `<Navigate to="/login" />`；
   - `canAccessPath()` 返回 false 且当前不在首页 → `<Navigate to="/" />`。

> 新增页面时，**必须同时**在 `router.tsx` 注册路由、在 `MENU_CONFIG` 或 `ROUTE_PERMISSIONS` 中登记权限，否则会出现"菜单点不进去"或"点进去 404"。

---

## 接口层约定

### Axios 实例（`src/utils/request.ts`）

```ts
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});
```

- **请求拦截**：从 `localStorage` 取 `token`，先本地解析 JWT 的 `exp`（提前 60 秒视为过期），过期则直接跳登录页、不发请求；未过期则注入 `Authorization: Bearer <token>`。
- **响应拦截**：后端统一返回 `Result<T>`，`code === 1` 表示成功并**自动解包出 `data`**；`code !== 1` 抛 `ApiError`。因此接口层的返回类型直接写业务类型 `T`。
- **错误处理**：`401` / `403` 清除 `token` 与 `user_info` 并跳转 `/login`（登录接口自身的 401 提示"账号或密码错误"）；超时与网络错误给出中文提示。
- **文件下载**：`downloadFile()` 使用裸 `axios` + `responseType: 'blob'`，绕开"解包 Result"的拦截器；后端若返回 JSON 错误会自动解析并抛出。

### 调用方式

```ts
import { http } from '../utils/request';

// 返回值已被解包，直接就是 Result.data 的类型
export const getCourse = (courseId: number) =>
  http.get<CourseVO>(`/course/${courseId}`);
```

### 接口分组

| 文件 | 业务域 | 典型接口 |
|------|--------|---------|
| `api/auth.ts` | 认证 | `POST /login` |
| `api/course.ts` | 课程 | `/course/page`、`/course/create`、`/course/update`、`/course/audit`、`/course/copy`、`/course/chapter/*`、`/course/resource/*` |
| `api/assessment.ts` | 任务 | `/task/list/{courseId}`、`/task/create`、`/task/gradeDesign/*` |
| | 题库 | `/question/list`、`/question/create`、`/question/import/upload` |
| | 试卷 | `/paper/list/{courseId}`、`/paper/autoGenerate`、`/paper/publish` |
| | 提交与批阅 | `/submission/start`、`/submission/submit`、`/submission/grade`、`/submission/excellent` |
| | 成绩 | `/gradeSummary/summarize/{courseId}`、`/gradeSummary/confirm/{courseId}`、`/gradeSummary/export/{courseId}` |
| | 申诉 | `/gradeAppeal/create`、`/gradeAppeal/review` |
| | 防作弊 | `/cheatLog/record`、`/cheatLog/list/{submissionId}` |
| `api/user.ts` | 用户 | ⚠️ 暂无生效接口（`/user/info`、`/user/password`、`/user/profile` 均为注释） |

### React Query

`src/utils/queryClient.ts` 全局配置：`retry: 1`、`staleTime: 60_000`、`refetchOnWindowFocus: false`。所有读接口通过 `hooks/` 中的 `useXxx` 查询封装，写操作后用 `invalidateQueries` 刷新。

---

## 状态管理

全局状态只有一份：`src/store/authStore.ts`（Zustand，未使用 `persist` 中间件）。

| 字段 / 方法 | 说明 |
|------------|------|
| `user` | 登录返回的用户信息（`token` / `userId` / `username` / `realName` / `roleCode` / `roleLevel`） |
| `isAuthenticated` / `initialized` | 登录态标记、是否已从本地恢复 |
| `login()` / `logout()` | 登录写入、登出清理（`logout` 不调用后端接口） |
| `restore()` | 应用启动时从 `localStorage` 恢复登录态 |
| `getRoleCode()` / `getRoleLevel()` | 角色读取 |

持久化方式为**手动读写 `localStorage`**：`token` 与 `user_info` 两个键，与 `request.ts` 中的读取保持一致。

服务端状态（列表、详情）全部交给 React Query 管理，不放 Zustand。

---

## 自定义 Hooks

| Hook | 作用 |
|------|------|
| `useAuth()` | 从 `authStore` 派生 `userId` / `username` / `realName` / `roleCode` / `roleLevel` / `hasRole` / `isAdmin` |
| `useCourses()` | `useCourses`、`useCourse`、`useChapterTree` 查询封装 |
| `useAssessment()` | 任务、题库、导入记录、试卷、提交、优秀范本、申诉、成绩、防作弊等查询 |
| `useCountdown()` | 依据服务端下发的截止时间逐秒倒计时，到点只触发一次回调 |
| `useCheatRecorder()` | 监听 `visibilitychange` / `blur` / `copy` / `paste` / `contextmenu`，同类事件 3 秒去重，缓冲后批量提交 `/cheatLog/record` |

---

## 页面与组件

### 教师端 / 管理员

| 页面 | 功能 |
|------|------|
| `course/CourseList` | 课程分页与筛选、新建、审核、复制 |
| `course/CourseDetail` | 课程信息编辑、章节树增删、资源增删 |
| `assessment/TaskList` | 选课后管理任务：类型、发布方式、截止时间、成绩权重、迟交与重交 |
| `assessment/QuestionBank` | 题库分页筛选、五种题型增删改、xlsx/xls/docx 导入、导入记录与错误明细 |
| `assessment/PaperManage` | 手动选题 + 按题型/难度/数量/分值组卷、编辑、发布为任务 |
| `assessment/SubmissionReview` | 提交列表与状态筛选、逐题批阅打分、优秀范本、防作弊统计与明细 |
| `assessment/GradeManage` | 配置成绩构成权重（合计需为 100）、执行汇总、确认排名、导出 Excel |
| `assessment/AppealReview` | 按课程与状态处理申诉（通过须改分，驳回须填写理由） |

### 学生端

| 页面 | 功能 |
|------|------|
| `course/MyCourses` | 已发布课程卡片列表、前端搜索、进入详情 |
| `myAssessment/TaskList` | 测验任务列表，状态本地推算（未开始 / 作答中 / 待批阅 / 已批阅 / 已截止），支持开始、继续、重考 |
| `myAssessment/ExamPage` | 限时确认、倒计时自动交卷、`localStorage` 草稿恢复、防作弊上报、交卷结果与逐题解析 |
| `myAssessment/Grades` | 本人成绩汇总与测验明细 |
| `myAssessment/MyAppeals` | 新建申诉（课程 → 任务 → 已批阅提交）+ 我的申诉列表 |

### 通用

| 文件 | 功能 |
|------|------|
| `Home` | 欢迎页与统计占位（暂无接口，数字恒为 0） |
| `Profile` | 只读展示当前用户信息，编辑/改密/设备管理按钮为禁用状态 |
| `login` | 账号密码登录，成功后跳转 |

### 组件（仅 3 个）

| 组件 | 作用 |
|------|------|
| `CourseSelect` | 课程下拉，`studentMode` 只取已发布课程，`allowOwnOnly` 按 `teacherId` 过滤 |
| `QuestionCard` | 题干、选项、参考答案、解析，附作答插槽与批阅结果 |
| `AnswerInput` | 按 `qType` 渲染单选/多选/判断/填空/简答，答案编码与后端对齐（下标、`"0,2"`、`"1/0"`） |

---

## 开发约定

- **接口不写在页面里**：所有请求收敛到 `src/api/`，页面通过 `hooks/` 里的 React Query 封装调用。
- **类型先行**：`src/api/types.ts` 集中定义 `Result` / `PageResult` 与全部 VO、DTO；组件 props 必须有类型。
- **角色判断**：使用 `ROLES` 常量与 `useAuth()`，不要硬编码角色字符串。
- **新增页面四步**：建 `pages/` 子目录并默认导出 → 在 `router.tsx` 注册（`lazy`）→ 在 `MENU_CONFIG` / `ROUTE_PERMISSIONS` 登记权限 → 需要入口时补菜单项。
- **表单校验**统一用 Ant Design `Form` 的 `rules`，错误提示走中文文案。
- **加载态**使用骨架屏或 `Spin`，避免长时间白屏。

---

## 已知问题

| 问题 | 影响 | 建议 |
|------|------|------|
| 菜单项指向不存在的路由 | `MENU_CONFIG` 中的 `/learning`、`/tools/notebook`、`/tools/mindmap`、`/admin/users`、`/admin/roles` 在 `router.tsx` 中没有对应路由，点击落入 404 | 补占位页，或先移除这些菜单项 |
| 学生打不开课程详情 | `/courses` 的角色限制配合前缀匹配，使 `/courses/:id` 对学生一律拒绝，学生从"我的课程"点卡片会被弹回首页 | 在 `ROUTE_PERMISSIONS` 中为 `/courses/:id` 显式放行学生，或把 `canAccessPath` 改为精确匹配 |
| 个人中心接口未接 | `src/api/user.ts` 全是注释，页面只读 | 后端补齐 `/user/info` 等接口后取消注释并接入 |
| 首页统计为占位 | 4 个统计卡片恒为 0 | 接入统计接口或先隐藏 |
| `index.html` 元信息未定制 | `lang="en"`、`<title>` 仍是目录名 | 改为 `zh-CN` 与"智学汇平台" |
| 脚手架残留资源 | `src/App.css`、`src/assets/hero.png`、`react.svg`、`vite.svg` 均未被引用 | 确认后删除 |
| `dist/` 中存在旧构建产物 | 已由 `.gitignore` 排除，不影响仓库 | 无需处理，重新构建会覆盖 |

---

## 相关文档

- [../README.md](../README.md) — 仓库总览、整体架构、已实现范围
- [../docs/功能整合文档.md](../docs/功能整合文档.md) — 需求基线
- [功能分工.md](功能分工.md) — 前端团队分工与阶段计划
- [README.en.md](README.en.md) — 本文件英文版
