import { useQueries, useQuery } from '@tanstack/react-query';
import {
  getAppealPageByCourse,
  getCheatLogs,
  getExcellentPage,
  getGradeDesign,
  getGradeSummaryPage,
  getMyAppealPage,
  getPaper,
  getPaperPage,
  getQuestionImportPage,
  getQuestionPage,
  getSubmission,
  getSubmissionPage,
  getTask,
  getTaskList,
} from '../api/assessment';
import type { QuestionQueryParams, SubmissionVO, TaskVO } from '../api/types';

// 课程任务列表（全量）
export function useTaskList(courseId: number | null) {
  return useQuery({
    queryKey: ['taskList', courseId],
    queryFn: () => getTaskList(courseId!),
    enabled: !!courseId,
  });
}

// 任务详情
export function useTask(taskId: number | null) {
  return useQuery({
    queryKey: ['task', taskId],
    queryFn: () => getTask(taskId!),
    enabled: !!taskId,
  });
}

// 课程评分设计
export function useGradeDesign(courseId: number | null) {
  return useQuery({
    queryKey: ['gradeDesign', courseId],
    queryFn: () => getGradeDesign(courseId!),
    enabled: !!courseId,
  });
}

// 题库分页（后端 PageResult 只有 records+total，分页用本地 pageNum/pageSize）
export function useQuestionPage(
  courseId: number | null,
  pageNum: number,
  pageSize: number,
  query: QuestionQueryParams = {}
) {
  return useQuery({
    queryKey: ['questions', courseId, pageNum, pageSize, query],
    queryFn: () => getQuestionPage(courseId!, pageNum, pageSize, query),
    enabled: !!courseId,
  });
}

// 导入记录分页
export function useQuestionImportPage(courseId: number | null, pageNum: number, pageSize: number) {
  return useQuery({
    queryKey: ['questionImports', courseId, pageNum, pageSize],
    queryFn: () => getQuestionImportPage(courseId!, pageNum, pageSize),
    enabled: !!courseId,
  });
}

// 试卷分页
export function usePaperPage(courseId: number | null, pageNum: number, pageSize: number) {
  return useQuery({
    queryKey: ['papers', courseId, pageNum, pageSize],
    queryFn: () => getPaperPage(courseId!, pageNum, pageSize),
    enabled: !!courseId,
  });
}

// 试卷详情（含题目）
export function usePaper(paperId: number | null) {
  return useQuery({
    queryKey: ['paper', paperId],
    queryFn: () => getPaper(paperId!),
    enabled: !!paperId,
  });
}

// 任务提交分页
export function useSubmissionPage(
  taskId: number | null,
  pageNum: number,
  pageSize: number,
  status?: string
) {
  return useQuery({
    queryKey: ['submissions', taskId, pageNum, pageSize, status],
    queryFn: () => getSubmissionPage(taskId!, pageNum, pageSize, status),
    enabled: !!taskId,
  });
}

// 提交详情（含逐题作答）
export function useSubmission(submissionId: number | null) {
  return useQuery({
    queryKey: ['submission', submissionId],
    queryFn: () => getSubmission(submissionId!),
    enabled: !!submissionId,
  });
}

// 优秀范本分页
export function useExcellentPage(taskId: number | null, pageNum: number, pageSize: number) {
  return useQuery({
    queryKey: ['excellent', taskId, pageNum, pageSize],
    queryFn: () => getExcellentPage(taskId!, pageNum, pageSize),
    enabled: !!taskId,
  });
}

// 我的申诉分页
export function useMyAppealPage(pageNum: number, pageSize: number) {
  return useQuery({
    queryKey: ['myAppeals', pageNum, pageSize],
    queryFn: () => getMyAppealPage(pageNum, pageSize),
  });
}

// 课程申诉分页（教师）
export function useAppealPage(
  courseId: number | null,
  pageNum: number,
  pageSize: number,
  status?: string
) {
  return useQuery({
    queryKey: ['appeals', courseId, status, pageNum, pageSize],
    queryFn: () => getAppealPageByCourse(courseId!, pageNum, pageSize, status),
    enabled: !!courseId,
  });
}

// 成绩汇总分页
export function useGradeSummaryPage(courseId: number | null, pageNum: number, pageSize: number) {
  return useQuery({
    queryKey: ['gradeSummary', courseId, pageNum, pageSize],
    queryFn: () => getGradeSummaryPage(courseId!, pageNum, pageSize),
    enabled: !!courseId,
  });
}

// 提交的防作弊记录
export function useCheatLogs(submissionId: number | null) {
  return useQuery({
    queryKey: ['cheatLogs', submissionId],
    queryFn: () => getCheatLogs(submissionId!),
    enabled: !!submissionId,
  });
}

/**
 * 学生任务状态 N+1 查询：
 * 后端没有「我的提交」端点，按任务逐个拉取提交列表（pageSize 100），
 * 汇总为 Record<taskId, SubmissionVO[]>，页面再按 studentId 过滤出本人提交。
 */
export function useMySubmissionStatuses(tasks: TaskVO[] | undefined, userId: number | null) {
  return useQueries({
    queries: (tasks ?? []).map((t) => ({
      queryKey: ['mySubmissions', t.taskId],
      queryFn: () => getSubmissionPage(t.taskId, 1, 100),
      enabled: !!userId,
    })),
    combine: (results) => {
      const byTask: Record<number, SubmissionVO[]> = {};
      (tasks ?? []).forEach((t, i) => {
        byTask[t.taskId] = results[i]?.data?.records ?? [];
      });
      return {
        byTask,
        isLoading: results.some((r) => r.isLoading),
      };
    },
  });
}
