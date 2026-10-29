# Zhixuehui Platform · Frontend

Frontend for the Integrated Learning Platform — built with **React 19 + TypeScript + Vite 8 + Ant Design 6**. It currently covers two business lines: **course management** and **task & assessment** (including timed online exams with anti-cheating logging).

> Repository overview: [../README.md](../README.md) · Requirements baseline: [../docs/功能整合文档.md](../docs/功能整合文档.md) (Chinese)

---

## Status

| Capability | Status |
|------------|:------:|
| Login / JWT auth | ✅ |
| Course management (list, create, edit, audit, copy, chapters, resources) | ✅ |
| Student course list | 🚧 (course detail page blocked by a permission bug) |
| Tasks, question bank, papers, auto paper generation | ✅ |
| Timed online exam, auto-submit, local draft, anti-cheat log | ✅ |
| Grading, grade weighting, summary, Excel export | ✅ |
| Grade appeals (student → teacher review) | ✅ |
| Profile page | 🚧 (API layer still commented out, read-only) |
| AI notebook / video player / community / CET / admin console | 📋 planned |

---

## Tech stack

| Concern | Choice |
|---------|--------|
| Framework | React 19 |
| Language | TypeScript 6 (no `strict`, no path aliases) |
| Build | Vite 8 — React Compiler preset + Tailwind CSS 4 |
| UI | Ant Design 6 + `@ant-design/icons` |
| Routing | React Router 7 (every page is `React.lazy` loaded) |
| State | Zustand 5 for auth state, TanStack React Query 5 for server state |
| HTTP | Axios with a unified `Result<T>` unwrapping interceptor |
| Lint | ESLint 10 flat config + typescript-eslint |

---

## Project structure

```
src/
├── api/          # One file per domain: auth, course, assessment, types, user
├── components/assessment/   # AnswerInput, CourseSelect, QuestionCard
├── constants/    # roles, course, assessment enums and labels
├── hooks/        # useAuth, useCourses, useAssessment, useCountdown, useCheatRecorder
├── layouts/      # MainLayout (shell + guards), AuthLayout (login shell)
├── pages/        # Home, Profile, login, course/, assessment/, myAssessment/
├── router/       # Route table + router instance
├── store/        # authStore (Zustand)
└── utils/        # request.ts, queryClient.ts, permission.ts
```

---

## Getting started

Prerequisites: **Node.js ≥ 20.19** (22 LTS recommended) and the backend running on `http://localhost:8080`.

```bash
npm install
cp .env.example .env      # Windows: copy .env.example .env
npm run dev               # http://localhost:5173
```

| Script | Purpose |
|--------|---------|
| `npm run dev` | Vite dev server (port 5173) |
| `npm run build` | `tsc -b && vite build` → `dist/` |
| `npm run preview` | Preview the production build |
| `npm run lint` | ESLint over `**/*.{ts,tsx}` |

---

## Environment variables

Only `VITE_`-prefixed variables reach the browser. **Never put secrets here.**

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | Backend base URL. Use `http://localhost:8080` locally; leave empty when served behind a gateway that proxies `/api` |

There is no `server.proxy` in `vite.config.ts` — the app calls the backend directly and relies on the backend's permissive CORS config for local development. If the backend tightens CORS, add a dev proxy instead of loosening it further.

---

## Routes and permissions

Five roles, ordered by privilege: `super_admin`(1) → `dept_admin`(2) → `teacher`(3) → `assistant`(4) → `student`(5), mirroring `RoleConstant.java` on the backend.

| Path | Page | Allowed roles |
|------|------|---------------|
| `/` | Home | any signed-in user |
| `/profile` | Profile | any signed-in user |
| `/courses`, `/courses/:id` | CourseList / CourseDetail | admins, teacher, assistant |
| `/my-courses` | MyCourses | student |
| `/assessment/{tasks,questions,papers,grades,appeals}` | teacher-side assessment | admins, teacher, assistant |
| `/assessment/tasks/:taskId/submissions` | SubmissionReview | admins, teacher, assistant |
| `/my-assessment/{tasks,grades,appeals}`, `/my-assessment/exam/:taskId` | student-side assessment | student |
| `/login` | Login | anonymous |
| `*` | inline 404 | — |

There is no `<RequireAuth>` component and no route `meta`. Authorization lives in `src/utils/permission.ts` (`MENU_CONFIG`, `ROUTE_PERMISSIONS`, `filterMenuByRole`, `canAccessPath`) and is enforced inside `MainLayout`, which redirects anonymous users to `/login` and unauthorized paths to `/`.

A new page therefore needs **both** a route in `router.tsx` **and** a permission entry, otherwise it is either unreachable or lands on the 404 page.

---

## API conventions

`src/utils/request.ts` creates a single Axios instance:

- `baseURL: import.meta.env.VITE_API_BASE_URL || '/api'`, `timeout: 10000`
- Request interceptor reads `token` from `localStorage`, pre-checks the JWT `exp` (60 s early), and injects `Authorization: Bearer <token>`
- Response interceptor unwraps the backend envelope `Result<T>` — `code === 1` means success and the caller receives `data` directly; anything else throws `ApiError`
- `401` / `403` clear credentials and redirect to `/login`; `downloadFile()` uses a bare Axios instance for blob exports

All reads go through React Query wrappers in `src/hooks/`; pages never call `axios` directly.

---

## Known issues

- Menu entries `/learning`, `/tools/notebook`, `/tools/mindmap`, `/admin/users`, `/admin/roles` have no matching route and fall through to the 404 page.
- Students cannot open `/courses/:id`: the `/courses` role restriction plus prefix matching rejects them, so clicking a card in "My courses" bounces back to the home page.
- `src/api/user.ts` contains only commented-out endpoints, so the profile page is read-only.
- `index.html` still has `lang="en"` and the directory name as `<title>`.
- Unreferenced scaffold leftovers: `src/App.css`, `src/assets/hero.png`, `react.svg`, `vite.svg`.

---

## Related documents

- [../README.md](../README.md) — repository overview and quick start
- [README.md](README.md) — this document in Chinese (more detailed)
- [../docs/功能整合文档.md](../docs/功能整合文档.md) — requirements baseline (Chinese)
