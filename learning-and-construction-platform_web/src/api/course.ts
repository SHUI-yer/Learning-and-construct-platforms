import http from '../utils/request';
import type {
  ChapterParams,
  ChapterSortParams,
  ChapterTreeVO,
  ContentResourceParams,
  CourseAuditParams,
  CourseCopyParams,
  CourseCreateParams,
  CourseImportItem,
  CourseQueryParams,
  CourseUpdateParams,
  CourseVO,
  PageResult,
} from './types';

// 课程分页列表：GET /course/page
export function getCoursePage(
  pageNum: number,
  pageSize: number,
  query: CourseQueryParams = {}
): Promise<PageResult<CourseVO>> {
  return http.get<PageResult<CourseVO>>('/course/page', { params: { pageNum, pageSize, ...query } });
}

// 课程详情：GET /course/{courseId}（课程不存在时后端返回 data:null，不报错）
export function getCourseInfo(courseId: number): Promise<CourseVO | null> {
  return http.get<CourseVO | null>(`/course/${courseId}`);
}

// 教师自主创建课程：POST /course/create，返回新课程 id
export function createCourse(params: CourseCreateParams): Promise<number> {
  return http.post<number>('/course/create', params);
}

// 管理员导入创建课程：POST /course/import
export function importCourses(items: CourseImportItem[]): Promise<void> {
  return http.post<void>('/course/import', items);
}

// 课程审核（管理员）：POST /course/audit
export function auditCourse(params: CourseAuditParams): Promise<boolean> {
  return http.post<boolean>('/course/audit', params);
}

// 复制课程：POST /course/copy，返回新课程 id
export function copyCourse(params: CourseCopyParams): Promise<number> {
  return http.post<number>('/course/copy', params);
}

// 课程编辑：PUT /course/update
export function updateCourse(params: CourseUpdateParams): Promise<void> {
  return http.put<void>('/course/update', params);
}

// ============ 章节接口 ============

// 新增章节：POST /course/chapter/add
export function addChapter(params: ChapterParams): Promise<void> {
  return http.post<void>('/course/chapter/add', params);
}

// 更新章节：PUT /course/chapter/update
export function updateChapter(params: ChapterParams): Promise<void> {
  return http.put<void>('/course/chapter/update', params);
}

// 删除章节：DELETE /course/chapter/{chapterId}
export function deleteChapter(chapterId: number): Promise<void> {
  return http.delete<void>(`/course/chapter/${chapterId}`);
}

// 章节树：GET /course/chapter/tree/{courseId}
export function getChapterTree(courseId: number): Promise<ChapterTreeVO[]> {
  return http.get<ChapterTreeVO[]>(`/course/chapter/tree/${courseId}`);
}

// 章节拖拽排序：PUT /course/chapter/sort
export function sortChapter(params: ChapterSortParams): Promise<void> {
  return http.put<void>('/course/chapter/sort', params);
}

// ============ 资源接口 ============

// 新增资源：POST /course/resource/add
export function addResource(params: ContentResourceParams): Promise<void> {
  return http.post<void>('/course/resource/add', params);
}

// 更新资源：PUT /course/resource/update
export function updateResource(params: ContentResourceParams): Promise<void> {
  return http.put<void>('/course/resource/update', params);
}

// 删除资源：DELETE /course/resource/{resourceId}
export function deleteResource(resourceId: number): Promise<void> {
  return http.delete<void>(`/course/resource/${resourceId}`);
}
