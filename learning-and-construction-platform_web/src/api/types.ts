// 对照后端 lp-common / lp-auth / lp-course 的 Result、VO、DTO 定义。
// 后端 JSON 序列化为 camelCase，Long -> number，LocalDateTime -> string。

// ============ 统一响应结构 ============

// lp-common/result/Result.java：code=1 成功，0 及其它数字为失败
export interface Result<T> {
  code: number;
  msg: string | null;
  data: T;
}

// lp-common/result/PageResult.java
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

// ============ 认证 auth（lp-auth） ============

export interface LoginParams {
  username: string;
  password: string;
}

// LoginVO：角色层级 1超级管理员 2分院管理员 3任课教师 4助教 5学生
export interface LoginResult {
  token: string;
  userId: number;
  username: string;
  realName: string;
  roleCode: string;
  roleLevel: number;
}

// ============ 课程 course（lp-course） ============

// CourseVO
export interface CourseVO {
  courseId: number;
  courseName: string;
  courseCode: string;
  teacherId: number | null;
  deptId: number | null;
  status: number | null;
  semester: string | null;
  isPublic: number | null;
  coverUrl: string | null;
  description: string | null;
  version: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}

// CourseQueryDTO：GET /course/page 的查询参数
export interface CourseQueryParams {
  courseName?: string;
  status?: number;
  deptId?: number;
  semester?: string;
}

// CourseCreateDTO
export interface CourseCreateParams {
  courseName: string;
  courseCode?: string;
  deptId?: number;
  semester?: string;
  isPublic?: number;
  coverUrl?: string;
  description?: string;
}

// CourseUpdateDTO
export interface CourseUpdateParams extends CourseCreateParams {
  courseId: number;
  version?: number;
}

// CourseAuditDTO
export interface CourseAuditParams {
  courseId: number;
  auditStatus: number;
  auditRemark?: string;
}

// CourseCopyDTO
export interface CourseCopyParams {
  sourceCourseId: number;
  courseName?: string;
  courseCode?: string;
  deptId?: number;
  semester?: string;
  isPublic?: number;
  coverUrl?: string;
  description?: string;
  targetCourseId?: number;
}

// CourseImportItemDTO
export interface CourseImportItem {
  courseName: string;
  courseCode?: string;
  deptId?: number;
  semester?: string;
  isPublic?: number;
  coverUrl?: string;
  description?: string;
  teacherId: number;
  studentIdList?: number[];
  classIdList?: number[];
}

// ============ 章节 chapter ============

// ContentResource 实体（ChapterTreeVO.resourceList 的元素）
export interface ContentResource {
  resourceId: number;
  chapterId: number;
  resourceType: string | null;
  title: string | null;
  url: string | null;
  duration: number | null;
  contentText: string | null;
  sortOrder: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}

// ChapterTreeVO
export interface ChapterTreeVO {
  chapterId: number;
  courseId: number;
  title: string;
  sortOrder: number | null;
  parentId: number | null;
  children: ChapterTreeVO[];
  resourceList: ContentResource[];
}

// ChapterDTO（add/update 共用，add 时不需要 chapterId）
export interface ChapterParams {
  chapterId?: number;
  courseId: number;
  title: string;
  sortOrder?: number;
  parentId?: number;
}

// ChapterSortDTO
export interface ChapterSortParams {
  chapterId: number;
  targetSortOrder: number;
}

// ContentResourceDTO（add/update 共用）
export interface ContentResourceParams {
  resourceId?: number;
  chapterId: number;
  resourceType?: string;
  title?: string;
  url?: string;
  duration?: number;
  contentText?: string;
  sortOrder?: number;
}

// ============ 考核与考试 assessment（lp-assessment） ============

// ---- 任务 Task ----

// TaskVO
export interface TaskVO {
  taskId: number;
  courseId: number;
  chapterId: number | null;
  title: string;
  description: string | null;
  /** assignment-作业 / checkin-签到 / quiz-测验 / experiment-实验报告 */
  taskType: string;
  tag: string | null;
  weight: number | null;
  /** immediate-立即 / scheduled-定时 */
  publishType: string;
  publishTime: string | null;
  deadline: string | null;
  allowLate: number | null;
  latePenalty: number | null;
  maxScore: number | null;
  allowResubmit: number | null;
  attachmentUrls: string | null;
  createdAt: string;
  updatedAt: string;
}

// TaskCreateDTO
export interface TaskCreateParams {
  taskId?: number;
  courseId: number;
  chapterId?: number | null;
  title: string;
  description?: string | null;
  taskType: string;
  tag?: string | null;
  weight?: number | null;
  publishType: string;
  publishTime?: string | null;
  deadline: string;
  allowLate?: number | null;
  latePenalty?: number | null;
  maxScore?: number | null;
  allowResubmit?: number | null;
  attachmentUrls?: string | null;
}

// 更新任务：taskId 必填
export type TaskUpdateParams = TaskCreateParams & { taskId: number };

// ---- 题库 Question ----

// QuestionVO
export interface QuestionVO {
  qId: number;
  courseId: number;
  creatorId: number | null;
  /** single_choice 单选 / multiple_choice 多选 / judge 判断 / fill_blank 填空 / short_answer 简答 */
  qType: string;
  content: string;
  options: string[];
  /** 选择题为选项下标（多选逗号分隔），判断为 1/0 */
  answer: string;
  analysis: string | null;
  /** 1-简单 2-中等 3-困难 */
  difficulty: number;
  tags: string[];
  /** manual-手动 / batch_import-批量导入 / ai_generated-AI生成 */
  source: string | null;
  /** 乐观锁版本号，编辑题目时必须回传 */
  version: number;
  createdAt: string;
  updatedAt: string;
}

// QuestionCreateDTO
export interface QuestionCreateParams {
  qId?: number;
  courseId: number;
  qType: string;
  content: string;
  options?: string[];
  answer: string;
  analysis?: string | null;
  difficulty?: number;
  tags?: string[];
  /** 乐观锁版本号，编辑时必填 */
  version?: number;
}

export type QuestionUpdateParams = QuestionCreateParams & { qId: number; version: number };

// GET /question/list 查询条件
export interface QuestionQueryParams {
  qType?: string;
  difficulty?: number;
  keyword?: string;
}

// ImportErrorVO
export interface ImportErrorVO {
  rowNo: number;
  reason: string;
}

// QuestionImportVO
export interface QuestionImportVO {
  id: number;
  courseId: number;
  operatorId: number | null;
  fileUrl: string | null;
  fileFormat: string | null;
  importCount: number | null;
  successCount: number | null;
  failCount: number | null;
  errors: ImportErrorVO[] | null;
  status: string | null;
  createdAt: string;
  updatedAt: string;
}

// ---- 试卷 Paper ----

// PaperQuestionVO（试卷内题目，含参考答案/解析）
export interface PaperQuestionVO {
  qId: number;
  qType: string;
  content: string;
  options: string[];
  answer: string | null;
  analysis: string | null;
  difficulty: number | null;
  score: number;
  sortOrder: number;
}

// ExamPaperVO
export interface ExamPaperVO {
  paperId: number;
  courseId: number;
  /** 发布后回写的任务ID；未发布为 null */
  taskId: number | null;
  title: string;
  totalScore: number | null;
  /** 限时分钟数，null/0 表示不限时 */
  duration: number | null;
  /** 1-乱序出题 0-否 */
  shuffleQuestions: number;
  version: number;
  questions: PaperQuestionVO[] | null;
  createdAt: string;
  updatedAt: string;
}

// PaperQuestionItemDTO
export interface PaperQuestionItem {
  qId: number;
  score: number;
}

// PaperCreateDTO
export interface PaperCreateParams {
  paperId?: number;
  courseId: number;
  title: string;
  duration?: number | null;
  shuffleQuestions?: number;
  version?: number;
  items?: PaperQuestionItem[];
}

export type PaperUpdateParams = PaperCreateParams & { paperId: number; version: number };

// PaperAutoRuleDTO
export interface PaperAutoGenerateRule {
  qType: string;
  difficulty?: number | null;
  count: number;
  score: number;
}

// PaperAutoGenerateDTO
export interface PaperAutoGenerateParams {
  courseId: number;
  title: string;
  duration?: number | null;
  shuffleQuestions?: number;
  rules: PaperAutoGenerateRule[];
}

// PaperPublishDTO（发布为任务）
export interface PaperPublishParams {
  paperId: number;
  courseId: number;
  title?: string | null;
  description?: string | null;
  /** quiz-测验/考试 assignment-作业 */
  taskType: string;
  tag?: string | null;
  weight?: number | null;
  publishType: string;
  publishTime?: string | null;
  deadline: string;
  allowLate?: number | null;
  latePenalty?: number | null;
  maxScore?: number | null;
  allowResubmit?: number | null;
}

// ---- 提交 Submission ----

// ExamStartVO
export interface ExamStartVO {
  submissionId: number;
  taskId: number;
  startedAt: string;
  /** 限时分钟数 */
  duration: number;
  /** 服务端计算的交卷截止时间 */
  deadline: string;
}

// ExamAnswerVO（逐题作答与判分）
export interface ExamAnswerVO {
  qId: number;
  qType: string | null;
  content: string | null;
  options: string[] | null;
  /** 参考答案 */
  answer: string | null;
  analysis: string | null;
  studentAnswer: string | null;
  /** 1-对 0-错 null-主观题待批阅 */
  isCorrect: number | null;
  score: number | null;
  fullScore: number | null;
  sortOrder: number | null;
}

// SubmissionVO
export interface SubmissionVO {
  submissionId: number;
  taskId: number;
  taskTitle: string | null;
  studentId: number;
  submitType: string | null;
  content: string | null;
  attachmentUrls: string | null;
  submittedAt: string;
  isLate: number | null;
  isExcellent: number | null;
  /** pending-待批阅 / in_progress-作答中 / graded-已批阅 */
  status: string;
  teacherComment: string | null;
  score: number | null;
  maxScore: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  answers: ExamAnswerVO[] | null;
}

// SubmissionSubmitDTO.AnswerItem
export interface SubmissionAnswerItem {
  qId: number;
  studentAnswer: string;
}

// SubmissionSubmitDTO
export interface SubmissionSubmitParams {
  taskId: number;
  answers: SubmissionAnswerItem[];
}

// SubmissionGradeDTO
export interface SubmissionGradeParams {
  submissionId: number;
  score: number;
  teacherComment?: string | null;
  version: number;
}

// SubmissionExcellentDTO
export interface SubmissionExcellentParams {
  submissionId: number;
  /** 1-设为范本 0-取消 */
  isExcellent: number;
  version: number;
}

// ---- 申诉 GradeAppeal ----

// GradeAppealVO
export interface GradeAppealVO {
  appealId: number;
  gradeId: number;
  courseId: number | null;
  taskId: number | null;
  taskTitle: string | null;
  studentId: number | null;
  currentScore: number | null;
  appealReason: string | null;
  teacherResponse: string | null;
  newScore: number | null;
  /** pending-待处理 / approved-已通过 / rejected-已驳回 */
  status: string;
  createdAt: string;
  resolvedAt: string | null;
  version: number;
  updatedAt: string;
}

// GradeAppealCreateDTO
export interface GradeAppealCreateParams {
  gradeId: number;
  appealReason: string;
}

// GradeAppealReviewDTO
export interface GradeAppealReviewParams {
  appealId: number;
  status: string;
  teacherResponse?: string | null;
  newScore?: number | null;
  version: number;
}

// ---- 成绩 GradeDesign / GradeSummary ----

// GradeDesignVO（成绩构成）
export interface GradeDesignVO {
  schemeId: number;
  courseId: number;
  componentType: string;
  weight: number;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

// GradeSummaryVO（成绩汇总）
export interface GradeSummaryVO {
  id: number;
  courseId: number;
  studentId: number;
  finalScore: number | null;
  rankInCourse: number | null;
  confirmedBy: number | null;
  confirmedAt: string | null;
  isExported: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

// ---- 防作弊 CheatLog ----

// CheatLogVO
export interface CheatLogVO {
  id: number;
  submissionId: number;
  /** tab_switch 切屏 / copy_paste 复制粘贴 / right_click 右键 / over_time 超时 */
  cheatType: string;
  cheatTypeName: string;
  detail: string | null;
  occurredAt: string;
}

// CheatLogRecordDTO.CheatEvent
export interface CheatEventItem {
  cheatType: string;
  detail?: string | null;
  occurredAt?: string | null;
}

// CheatLogRecordDTO
export interface CheatLogRecordParams {
  submissionId: number;
  events: CheatEventItem[];
}
