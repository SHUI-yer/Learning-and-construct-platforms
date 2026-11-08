import { lazy } from 'react';

export const Login = lazy(() => import('../pages/login'));
export const Home = lazy(() => import('../pages/Home'));
export const Profile = lazy(() => import('../pages/Profile'));
export const CourseList = lazy(() => import('../pages/course/CourseList'));
export const CourseDetail = lazy(() => import('../pages/course/CourseDetail'));
export const MyCourses = lazy(() => import('../pages/course/MyCourses'));

export const TaskList = lazy(() => import('../pages/assessment/TaskList'));
export const QuestionBank = lazy(() => import('../pages/assessment/QuestionBank'));
export const PaperManage = lazy(() => import('../pages/assessment/PaperManage'));
export const SubmissionReview = lazy(() => import('../pages/assessment/SubmissionReview'));
export const GradeManage = lazy(() => import('../pages/assessment/GradeManage'));
export const AppealReview = lazy(() => import('../pages/assessment/AppealReview'));

export const MyTaskList = lazy(() => import('../pages/myAssessment/TaskList'));
export const ExamPage = lazy(() => import('../pages/myAssessment/ExamPage'));
export const MyAppeals = lazy(() => import('../pages/myAssessment/MyAppeals'));
export const MyGrades = lazy(() => import('../pages/myAssessment/Grades'));

export function NotFound() {
  return (
    <div style={{ textAlign: 'center', paddingTop: 100 }}>
      <h1 style={{ fontSize: 72, margin: 0, color: '#d9d9d9' }}>404</h1>
      <p style={{ fontSize: 16, color: '#8c8c8c' }}>页面不存在或已被移除</p>
      <a href="/" style={{ color: '#1677ff' }}>返回首页</a>
    </div>
  );
}
