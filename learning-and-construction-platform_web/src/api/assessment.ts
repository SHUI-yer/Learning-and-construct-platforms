import http, { downloadFile, requestInstance } from '../utils/request';
import type {
  CheatLogRecordParams,
  CheatLogVO,
  ExamPaperVO,
  ExamStartVO,
  GradeAppealCreateParams,
  GradeAppealReviewParams,
  GradeAppealVO,
  GradeDesignVO,
  GradeSummaryVO,
  PaperAutoGenerateParams,
  PaperCreateParams,
  PaperPublishParams,
  PaperUpdateParams,
  PageResult,
  QuestionCreateParams,
  QuestionImportVO,
  QuestionQueryParams,
  QuestionUpdateParams,
  QuestionVO,
  SubmissionExcellentParams,
  SubmissionGradeParams,
  SubmissionSubmitParams,
  SubmissionVO,
  TaskCreateParams,
  TaskUpdateParams,
  TaskVO,
} from './types';

// ============ 任务 task ============

// 课程任务全量列表：GET /task/list/{courseId}（不分页）
export function getTaskList(courseId: number): Promise<TaskVO[]> {
  return http.get<TaskVO[]>(`/task/list/${courseId}`);
}

// 任务详情：GET /task/{taskId}
export function getTask(taskId: number): Promise<TaskVO> {
  return http.get<TaskVO>(`/task/${taskId}`);
}

// 创建任务：POST /task/create
export function createTask(params: TaskCreateParams): Promise<TaskVO> {
  return http.post<TaskVO>('/task/create', params);
}

// 更新任务：PUT /task/update
export function updateTask(params: TaskUpdateParams): Promise<TaskVO> {
  return http.put<TaskVO>('/task/update', params);
}

// 删除任务：DELETE /task/{taskId}
export function deleteTask(taskId: number): Promise<string> {
  return http.delete<string>(`/task/${taskId}`);
}

// 课程评分设计：GET /task/gradeDesign/{courseId}
export function getGradeDesign(courseId: number): Promise<GradeDesignVO[]> {
  return http.get<GradeDesignVO[]>(`/task/gradeDesign/${courseId}`);
}

// 保存课程评分设计：POST /task/gradeDesign/save（权重合计须为 100）
export function saveGradeDesign(
  courseId: number,
  items: { componentType: string; weight: number; sortOrder: number }[]
): Promise<GradeDesignVO[]> {
  return http.post<GradeDesignVO[]>('/task/gradeDesign/save', { courseId, items });
}

// ============ 题库 question ============

// 题目分页列表：GET /question/list（后端 PageResult 只有 records+total）
export function getQuestionPage(
  courseId: number,
  pageNum: number,
  pageSize: number,
  query: QuestionQueryParams = {}
): Promise<PageResult<QuestionVO>> {
  return http.get<PageResult<QuestionVO>>('/question/list', {
    params: { courseId, page: pageNum, pageSize, ...query },
  });
}

// 题目详情：GET /question/{qId}
export function getQuestion(qId: number): Promise<QuestionVO> {
  return http.get<QuestionVO>(`/question/${qId}`);
}

// 创建题目：POST /question/create
export function createQuestion(params: QuestionCreateParams): Promise<QuestionVO> {
  return http.post<QuestionVO>('/question/create', params);
}

// 更新题目：PUT /question/update
export function updateQuestion(params: QuestionUpdateParams): Promise<QuestionVO> {
  return http.put<QuestionVO>('/question/update', params);
}

// 删除题目：DELETE /question/{qId}
export function deleteQuestion(qId: number): Promise<string> {
  return http.delete<string>(`/question/${qId}`);
}

// 课程题目总数：GET /question/count/{courseId}
export function getQuestionCount(courseId: number): Promise<number> {
  return http.get<number>(`/question/count/${courseId}`);
}

// ============ 题库导入 questionImport ============

// 上传题目文件（.xlsx/.xls/.docx）：POST /question/import/upload
export async function uploadQuestionFile(courseId: number, file: File): Promise<QuestionImportVO> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('courseId', String(courseId));
  return requestInstance.post<QuestionImportVO>('/question/import/upload', formData, {
    timeout: 60000,
  });
}

// 导入记录分页：GET /question/import/list
export function getQuestionImportPage(
  courseId: number,
  pageNum: number,
  pageSize: number
): Promise<PageResult<QuestionImportVO>> {
  return http.get<PageResult<QuestionImportVO>>('/question/import/list', {
    params: { courseId, page: pageNum, pageSize },
  });
}

// 导入记录详情（含错误明细）：GET /question/import/{id}
export function getQuestionImportDetail(id: number): Promise<QuestionImportVO> {
  return http.get<QuestionImportVO>(`/question/import/${id}`);
}

// ============ 试卷 paper ============

// 试卷分页列表：GET /paper/list/{courseId}
export function getPaperPage(
  courseId: number,
  pageNum: number,
  pageSize: number
): Promise<PageResult<ExamPaperVO>> {
  return http.get<PageResult<ExamPaperVO>>(`/paper/list/${courseId}`, {
    params: { page: pageNum, pageSize },
  });
}

// 试卷详情（含题目）：GET /paper/{paperId}
export function getPaper(paperId: number): Promise<ExamPaperVO> {
  return http.get<ExamPaperVO>(`/paper/${paperId}`);
}

// 手动创建试卷：POST /paper/create
export function createPaper(params: PaperCreateParams): Promise<ExamPaperVO> {
  return http.post<ExamPaperVO>('/paper/create', params);
}

// 智能组卷：POST /paper/autoGenerate
export function autoGeneratePaper(params: PaperAutoGenerateParams): Promise<ExamPaperVO> {
  return http.post<ExamPaperVO>('/paper/autoGenerate', params);
}

// 更新试卷：PUT /paper/update
export function updatePaper(params: PaperUpdateParams): Promise<ExamPaperVO> {
  return http.put<ExamPaperVO>('/paper/update', params);
}

// 删除试卷：DELETE /paper/{paperId}
export function deletePaper(paperId: number): Promise<string> {
  return http.delete<string>(`/paper/${paperId}`);
}

// 发布试卷（生成关联任务）：POST /paper/publish
export function publishPaper(params: PaperPublishParams): Promise<TaskVO> {
  return http.post<TaskVO>('/paper/publish', params);
}

// ============ 提交 submission ============

// 开始限时作答：POST /submission/start?taskId=（进行中幂等；允许重交则重置计时）
export function startSubmission(taskId: number): Promise<ExamStartVO> {
  return http.post<ExamStartVO>('/submission/start', null, { params: { taskId } });
}

// 交卷（自动判分客观题）：POST /submission/submit
export function submitSubmission(params: SubmissionSubmitParams): Promise<SubmissionVO> {
  return http.post<SubmissionVO>('/submission/submit', params);
}

// 任务提交分页：GET /submission/list/{taskId}?status=pending|in_progress|graded
export function getSubmissionPage(
  taskId: number,
  pageNum: number,
  pageSize: number,
  status?: string
): Promise<PageResult<SubmissionVO>> {
  return http.get<PageResult<SubmissionVO>>(`/submission/list/${taskId}`, {
    params: { page: pageNum, pageSize, status },
  });
}

// 提交详情（含逐题作答）：GET /submission/{submissionId}
export function getSubmission(submissionId: number): Promise<SubmissionVO> {
  return http.get<SubmissionVO>(`/submission/${submissionId}`);
}

// 教师批阅打分：POST /submission/grade
export function gradeSubmission(params: SubmissionGradeParams): Promise<SubmissionVO> {
  return http.post<SubmissionVO>('/submission/grade', params);
}

// 设为/取消优秀范本：POST /submission/excellent
export function setSubmissionExcellent(params: SubmissionExcellentParams): Promise<SubmissionVO> {
  return http.post<SubmissionVO>('/submission/excellent', params);
}

// 优秀范本分页：GET /submission/excellent/list/{taskId}
export function getExcellentPage(
  taskId: number,
  pageNum: number,
  pageSize: number
): Promise<PageResult<SubmissionVO>> {
  return http.get<PageResult<SubmissionVO>>(`/submission/excellent/list/${taskId}`, {
    params: { page: pageNum, pageSize },
  });
}

// ============ 申诉 gradeAppeal ============

// 学生创建申诉：POST /gradeAppeal/create
export function createGradeAppeal(params: GradeAppealCreateParams): Promise<GradeAppealVO> {
  return http.post<GradeAppealVO>('/gradeAppeal/create', params);
}

// 我的申诉分页：GET /gradeAppeal/my
export function getMyAppealPage(pageNum: number, pageSize: number): Promise<PageResult<GradeAppealVO>> {
  return http.get<PageResult<GradeAppealVO>>('/gradeAppeal/my', { params: { page: pageNum, pageSize } });
}

// 课程申诉分页（教师）：GET /gradeAppeal/list/{courseId}
export function getAppealPageByCourse(
  courseId: number,
  pageNum: number,
  pageSize: number,
  status?: string
): Promise<PageResult<GradeAppealVO>> {
  return http.get<PageResult<GradeAppealVO>>(`/gradeAppeal/list/${courseId}`, {
    params: { page: pageNum, pageSize, status },
  });
}

// 申诉详情：GET /gradeAppeal/{appealId}
export function getGradeAppeal(appealId: number): Promise<GradeAppealVO> {
  return http.get<GradeAppealVO>(`/gradeAppeal/${appealId}`);
}

// 教师复核申诉：POST /gradeAppeal/review
export function reviewGradeAppeal(params: GradeAppealReviewParams): Promise<GradeAppealVO> {
  return http.post<GradeAppealVO>('/gradeAppeal/review', params);
}

// 提交对应的成绩记录ID：GET /gradeAppeal/grade/{submissionId}（无成绩记录返回 null）
export function getGradeIdBySubmission(submissionId: number): Promise<number | null> {
  return http.get<number | null>(`/gradeAppeal/grade/${submissionId}`);
}

// ============ 成绩汇总 gradeSummary ============

// 执行成绩汇总：POST /gradeSummary/summarize/{courseId}
export function summarizeGrades(courseId: number): Promise<GradeSummaryVO[]> {
  return http.post<GradeSummaryVO[]>(`/gradeSummary/summarize/${courseId}`);
}

// 汇总分页：GET /gradeSummary/list/{courseId}
export function getGradeSummaryPage(
  courseId: number,
  pageNum: number,
  pageSize: number
): Promise<PageResult<GradeSummaryVO>> {
  return http.get<PageResult<GradeSummaryVO>>(`/gradeSummary/list/${courseId}`, {
    params: { page: pageNum, pageSize },
  });
}

// 确认排名：POST /gradeSummary/confirm/{courseId}
export function confirmGradeSummary(courseId: number): Promise<GradeSummaryVO[]> {
  return http.post<GradeSummaryVO[]>(`/gradeSummary/confirm/${courseId}`);
}

// 导出成绩单 Excel：GET /gradeSummary/export/{courseId}（blob 下载）
export async function exportGradeSummary(courseId: number): Promise<void> {
  return downloadFile(`/gradeSummary/export/${courseId}`, `成绩汇总_${courseId}.xlsx`);
}

// ============ 防作弊 cheatLog ============

// 记录防作弊事件（批量）：POST /cheatLog/record
export function recordCheatLog(params: CheatLogRecordParams): Promise<CheatLogVO[]> {
  return http.post<CheatLogVO[]>('/cheatLog/record', params);
}

// 提交的违规记录：GET /cheatLog/list/{submissionId}
export function getCheatLogs(submissionId: number): Promise<CheatLogVO[]> {
  return http.get<CheatLogVO[]>(`/cheatLog/list/${submissionId}`);
}

// 违规次数统计：GET /cheatLog/count/{submissionId}?cheatType=
export function countCheatLogs(submissionId: number, cheatType?: string): Promise<number> {
  return http.get<number>(`/cheatLog/count/${submissionId}`, { params: { cheatType } });
}
