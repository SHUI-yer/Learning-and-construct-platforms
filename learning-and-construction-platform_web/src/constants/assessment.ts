/**
 * 考核与考试常量
 *
 * 对照后端 lp-assessment 的 entity/dto/service 中的字符串枚举定义。
 * 这些字符串直接落库，改动需同步后端。
 */

// ============ 题型 ============

/** 题型（QuestionValidator.Q_TYPES） */
export const QUESTION_TYPE = {
  SINGLE_CHOICE: 'single_choice',
  MULTIPLE_CHOICE: 'multiple_choice',
  JUDGE: 'judge',
  FILL_BLANK: 'fill_blank',
  SHORT_ANSWER: 'short_answer',
} as const;

export type QuestionType = (typeof QUESTION_TYPE)[keyof typeof QUESTION_TYPE];

/** 题型 → 中文标签 */
export const QUESTION_TYPE_LABELS: Record<string, string> = {
  [QUESTION_TYPE.SINGLE_CHOICE]: '单选题',
  [QUESTION_TYPE.MULTIPLE_CHOICE]: '多选题',
  [QUESTION_TYPE.JUDGE]: '判断题',
  [QUESTION_TYPE.FILL_BLANK]: '填空题',
  [QUESTION_TYPE.SHORT_ANSWER]: '简答题',
};

/** 题型 → Tag 颜色 */
export const QUESTION_TYPE_COLORS: Record<string, string> = {
  [QUESTION_TYPE.SINGLE_CHOICE]: 'blue',
  [QUESTION_TYPE.MULTIPLE_CHOICE]: 'geekblue',
  [QUESTION_TYPE.JUDGE]: 'cyan',
  [QUESTION_TYPE.FILL_BLANK]: 'orange',
  [QUESTION_TYPE.SHORT_ANSWER]: 'purple',
};

/** 客观题（可自动判分） */
export const OBJECTIVE_QUESTION_TYPES: string[] = [
  QUESTION_TYPE.SINGLE_CHOICE,
  QUESTION_TYPE.MULTIPLE_CHOICE,
  QUESTION_TYPE.JUDGE,
];

// ============ 难度 ============

/** 难度 → 中文标签 */
export const DIFFICULTY_LABELS: Record<number, string> = {
  1: '简单',
  2: '中等',
  3: '困难',
};

/** 难度 → Tag 颜色 */
export const DIFFICULTY_COLORS: Record<number, string> = {
  1: 'green',
  2: 'gold',
  3: 'red',
};

// ============ 任务类型 ============

/** 任务类型（task 表 task_type） */
export const TASK_TYPE = {
  ASSIGNMENT: 'assignment',
  CHECKIN: 'checkin',
  QUIZ: 'quiz',
  EXPERIMENT: 'experiment',
} as const;

export type TaskType = (typeof TASK_TYPE)[keyof typeof TASK_TYPE];

/** 任务类型 → 中文标签 */
export const TASK_TYPE_LABELS: Record<string, string> = {
  [TASK_TYPE.ASSIGNMENT]: '作业',
  [TASK_TYPE.CHECKIN]: '签到',
  [TASK_TYPE.QUIZ]: '测验/考试',
  [TASK_TYPE.EXPERIMENT]: '实验报告',
};

/** 任务类型 → Tag 颜色 */
export const TASK_TYPE_COLORS: Record<string, string> = {
  [TASK_TYPE.ASSIGNMENT]: 'blue',
  [TASK_TYPE.CHECKIN]: 'green',
  [TASK_TYPE.QUIZ]: 'volcano',
  [TASK_TYPE.EXPERIMENT]: 'purple',
};

// ============ 发布方式 ============

/** 发布方式（TaskServiceImpl.PUBLISH_*） */
export const PUBLISH_TYPE = {
  IMMEDIATE: 'immediate',
  SCHEDULED: 'scheduled',
} as const;

export type PublishType = (typeof PUBLISH_TYPE)[keyof typeof PUBLISH_TYPE];

/** 发布方式 → 中文标签 */
export const PUBLISH_TYPE_LABELS: Record<string, string> = {
  [PUBLISH_TYPE.IMMEDIATE]: '立即发布',
  [PUBLISH_TYPE.SCHEDULED]: '定时发布',
};

// ============ 提交状态 ============

/** 提交状态（SubmissionServiceImpl.STATUS_*） */
export const SUBMISSION_STATUS = {
  PENDING: 'pending',
  IN_PROGRESS: 'in_progress',
  GRADED: 'graded',
} as const;

export type SubmissionStatus = (typeof SUBMISSION_STATUS)[keyof typeof SUBMISSION_STATUS];

/** 提交状态 → 中文标签 */
export const SUBMISSION_STATUS_LABELS: Record<string, string> = {
  [SUBMISSION_STATUS.PENDING]: '待批阅',
  [SUBMISSION_STATUS.IN_PROGRESS]: '作答中',
  [SUBMISSION_STATUS.GRADED]: '已批阅',
};

/** 提交状态 → Tag 颜色 */
export const SUBMISSION_STATUS_COLORS: Record<string, string> = {
  [SUBMISSION_STATUS.PENDING]: 'orange',
  [SUBMISSION_STATUS.IN_PROGRESS]: 'processing',
  [SUBMISSION_STATUS.GRADED]: 'success',
};

// ============ 申诉状态 ============

/** 申诉状态（GradeAppealServiceImpl.STATUS_*） */
export const APPEAL_STATUS = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const;

export type AppealStatus = (typeof APPEAL_STATUS)[keyof typeof APPEAL_STATUS];

/** 申诉状态 → 中文标签 */
export const APPEAL_STATUS_LABELS: Record<string, string> = {
  [APPEAL_STATUS.PENDING]: '待处理',
  [APPEAL_STATUS.APPROVED]: '已通过',
  [APPEAL_STATUS.REJECTED]: '已驳回',
};

/** 申诉状态 → Tag 颜色 */
export const APPEAL_STATUS_COLORS: Record<string, string> = {
  [APPEAL_STATUS.PENDING]: 'orange',
  [APPEAL_STATUS.APPROVED]: 'success',
  [APPEAL_STATUS.REJECTED]: 'error',
};

// ============ 导入状态 ============

/** 导入状态（QuestionImportServiceImpl.STATUS_*） */
export const IMPORT_STATUS = {
  PROCESSING: 'processing',
  COMPLETED: 'completed',
  FAILED: 'failed',
} as const;

export type ImportStatus = (typeof IMPORT_STATUS)[keyof typeof IMPORT_STATUS];

/** 导入状态 → 中文标签 */
export const IMPORT_STATUS_LABELS: Record<string, string> = {
  [IMPORT_STATUS.PROCESSING]: '处理中',
  [IMPORT_STATUS.COMPLETED]: '已完成',
  [IMPORT_STATUS.FAILED]: '失败',
};

/** 导入状态 → Tag 颜色 */
export const IMPORT_STATUS_COLORS: Record<string, string> = {
  [IMPORT_STATUS.PROCESSING]: 'processing',
  [IMPORT_STATUS.COMPLETED]: 'success',
  [IMPORT_STATUS.FAILED]: 'error',
};

// ============ 防作弊 ============

/** 异常行为类型（CheatLogServiceImpl.CHEAT_TYPES，后端硬校验，只能发这四种） */
export const CHEAT_TYPE = {
  TAB_SWITCH: 'tab_switch',
  COPY_PASTE: 'copy_paste',
  RIGHT_CLICK: 'right_click',
  OVER_TIME: 'over_time',
} as const;

export type CheatType = (typeof CHEAT_TYPE)[keyof typeof CHEAT_TYPE];

/** 异常行为 → 中文名称（与后端 CheatLogVO.cheatTypeName 保持一致） */
export const CHEAT_TYPE_LABELS: Record<string, string> = {
  [CHEAT_TYPE.TAB_SWITCH]: '切屏切换',
  [CHEAT_TYPE.COPY_PASTE]: '复制粘贴',
  [CHEAT_TYPE.RIGHT_CLICK]: '右键异常',
  [CHEAT_TYPE.OVER_TIME]: '作答超时',
};

// ============ 工具函数 ============

/** 下标 -> 选项字母：0 -> A、1 -> B ... */
export function indexToLetter(index: number): string {
  return String.fromCharCode(65 + index);
}

/**
 * 参考答案展示：把后端存储的下标编码转成可读字母
 * 单选 "0" -> "A"；多选 "0,2" -> "A、C"；判断 "1" -> "正确"；填空/简答原样返回
 */
export function formatReferenceAnswer(
  qType: string | null | undefined,
  answer: string | null | undefined
): string {
  if (answer === null || answer === undefined || answer === '') return '-';

  if (qType === QUESTION_TYPE.JUDGE) {
    return answer === '1' ? '正确' : '错误';
  }

  if (qType === QUESTION_TYPE.SINGLE_CHOICE || qType === QUESTION_TYPE.MULTIPLE_CHOICE) {
    const letters = answer
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s !== '')
      .map((s) => {
        const idx = Number(s);
        // 后端也兼容 A-D 字母写法，这里只处理数字下标，非数字原样展示
        return Number.isInteger(idx) && idx >= 0 ? indexToLetter(idx) : s;
      });
    return letters.length > 0 ? letters.join('、') : '-';
  }

  return answer;
}
