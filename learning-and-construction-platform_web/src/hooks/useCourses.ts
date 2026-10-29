import { useQuery } from '@tanstack/react-query';
import { getChapterTree, getCourseInfo, getCoursePage } from '../api/course';
import type { CourseQueryParams } from '../api/types';

// 课程分页列表
export function useCourses(pageNum = 1, pageSize = 10, query: CourseQueryParams = {}) {
  return useQuery({
    queryKey: ['courses', pageNum, pageSize, query],
    queryFn: () => getCoursePage(pageNum, pageSize, query),
  });
}

// 课程详情
export function useCourse(courseId: number) {
  return useQuery({
    queryKey: ['course', courseId],
    queryFn: () => getCourseInfo(courseId),
  });
}

// 课程章节树
export function useChapterTree(courseId: number) {
  return useQuery({
    queryKey: ['chapterTree', courseId],
    queryFn: () => getChapterTree(courseId),
  });
}
