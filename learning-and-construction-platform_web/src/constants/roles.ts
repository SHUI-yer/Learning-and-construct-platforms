/**
 * 角色常量 —— 对照后端 RoleConstant.java
 *
 * roleLevel 数值越小权限越高：
 *   1 = super_admin  超级管理员
 *   2 = dept_admin   分院管理员
 *   3 = teacher      任课教师
 *   4 = assistant    助教
 *   5 = student      学生
 */

export const ROLES = {
  SUPER_ADMIN: 'super_admin',
  DEPT_ADMIN: 'dept_admin',
  TEACHER: 'teacher',
  ASSISTANT: 'assistant',
  STUDENT: 'student',
} as const;

export type RoleCode = (typeof ROLES)[keyof typeof ROLES];

/** 角色中文名称映射 */
export const ROLE_LABELS: Record<RoleCode, string> = {
  super_admin: '超级管理员',
  dept_admin: '分院管理员',
  teacher: '任课教师',
  assistant: '助教',
  student: '学生',
};

/** 角色等级映射 */
export const ROLE_LEVELS: Record<RoleCode, number> = {
  super_admin: 1,
  dept_admin: 2,
  teacher: 3,
  assistant: 4,
  student: 5,
};
