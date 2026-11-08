import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';
import {
  AppealReview,
  CourseDetail,
  CourseList,
  ExamPage,
  GradeManage,
  Home,
  Login,
  MyAppeals,
  MyCourses,
  MyGrades,
  MyTaskList,
  NotFound,
  PaperManage,
  Profile,
  QuestionBank,
  SubmissionReview,
  TaskList,
} from './lazyPages';

const routes = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'profile',
        element: <Profile />,
      },
      // 课程管理（教师 / 管理员）
      {
        path: 'courses',
        element: <CourseList />,
      },
      // 课程详情（所有角色可访问，权限由页面内部控制）
      {
        path: 'courses/:id',
        element: <CourseDetail />,
      },
      // 学生端：我的课程
      {
        path: 'my-courses',
        element: <MyCourses />,
      },
      // ===== 考核与考试（教师 / 管理员） =====
      {
        path: 'assessment/tasks',
        element: <TaskList />,
      },
      {
        path: 'assessment/questions',
        element: <QuestionBank />,
      },
      {
        path: 'assessment/papers',
        element: <PaperManage />,
      },
      // 阅卷：任务提交列表 + 批阅 + 防作弊记录（无独立菜单，从任务列表进入）
      {
        path: 'assessment/tasks/:taskId/submissions',
        element: <SubmissionReview />,
      },
      {
        path: 'assessment/grades',
        element: <GradeManage />,
      },
      {
        path: 'assessment/appeals',
        element: <AppealReview />,
      },
      // ===== 考核与考试（学生端） =====
      {
        path: 'my-assessment/tasks',
        element: <MyTaskList />,
      },
      {
        path: 'my-assessment/exam/:taskId',
        element: <ExamPage />,
      },
      {
        path: 'my-assessment/grades',
        element: <MyGrades />,
      },
      {
        path: 'my-assessment/appeals',
        element: <MyAppeals />,
      },
    ],
  },
  {
    path: '/login',
    element: <AuthLayout />,
    children: [
      {
        index: true,
        element: <Login />,
      },
    ],
  },
  // 404 兜底
  {
    path: '*',
    element: <NotFound />,
  },
];

export default routes;
