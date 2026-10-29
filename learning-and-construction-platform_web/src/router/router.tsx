import { lazy } from 'react';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';

// 懒加载页面组件
const Login = lazy(() => import('../pages/login'));
const Home = lazy(() => import('../pages/Home'));
const Profile = lazy(() => import('../pages/Profile'));
const CourseList = lazy(() => import('../pages/course/CourseList'));
const CourseDetail = lazy(() => import('../pages/course/CourseDetail'));
const MyCourses = lazy(() => import('../pages/course/MyCourses'));

// 考核与考试（教师端）
const TaskList = lazy(() => import('../pages/assessment/TaskList'));
const QuestionBank = lazy(() => import('../pages/assessment/QuestionBank'));
const PaperManage = lazy(() => import('../pages/assessment/PaperManage'));
const SubmissionReview = lazy(() => import('../pages/assessment/SubmissionReview'));
const GradeManage = lazy(() => import('../pages/assessment/GradeManage'));
const AppealReview = lazy(() => import('../pages/assessment/AppealReview'));

// 考核与考试（学生端）
const MyTaskList = lazy(() => import('../pages/myAssessment/TaskList'));
const ExamPage = lazy(() => import('../pages/myAssessment/ExamPage'));
const MyAppeals = lazy(() => import('../pages/myAssessment/MyAppeals'));
const MyGrades = lazy(() => import('../pages/myAssessment/Grades'));

/** 404 占位 */
const NotFound = () => (
  <div style={{ textAlign: 'center', paddingTop: 100 }}>
    <h1 style={{ fontSize: 72, margin: 0, color: '#d9d9d9' }}>404</h1>
    <p style={{ fontSize: 16, color: '#8c8c8c' }}>页面不存在或已被移除</p>
    <a href="/" style={{ color: '#1677ff' }}>返回首页</a>
  </div>
);

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
