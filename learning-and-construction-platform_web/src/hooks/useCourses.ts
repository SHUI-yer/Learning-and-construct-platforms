import { useQuery } from '@tanstack/react-query';
import { getChapterTree, getCourseInfo, getCoursePage } from '../api/course';
import type { CourseQueryParams } from '../api/types';
import { useAuthStore } from '../store/authStore';

// 课程分页列表
export function useCourses(pageNum = 1, pageSize = 10, query: CourseQueryParams = {}) {
  const user = useAuthStore((state) => state.user);
  return useQuery({
    queryKey: ['courses', pageNum, pageSize, query, user?.userId, user?.roleCode],
    queryFn: () => getCoursePage(pageNum, pageSize, query),
  });
}

// 课程详情
export function useCourse(courseId: number) {
  const user = useAuthStore((state) => state.user);
  return useQuery({
    queryKey: ['course', courseId, user?.userId, user?.roleCode],
    queryFn: () => getCourseInfo(courseId),
  });
}

// 课程章节树
export function useChapterTree(courseId: number) {
  const user = useAuthStore((state) => state.user);
  return useQuery({
    queryKey: ['chapterTree', courseId, user?.userId, user?.roleCode],
    queryFn: () => getChapterTree(courseId),
  });
}
