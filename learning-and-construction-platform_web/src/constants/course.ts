/**
 * 课程状态常量
 *
 * 对照后端 CourseVO.status 字段
 * 审核状态通过 auditStatus 区分：1=通过 2=驳回
 */

/** 课程发布状态 */
export const COURSE_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
} as const;

export type CourseStatus = (typeof COURSE_STATUS)[keyof typeof COURSE_STATUS];

/** 状态 → 中文标签 */
export const COURSE_STATUS_LABELS: Record<number, string> = {
  [COURSE_STATUS.DRAFT]: '草稿',
  [COURSE_STATUS.PENDING]: '待审核',
  [COURSE_STATUS.APPROVED]: '已发布',
  [COURSE_STATUS.REJECTED]: '已驳回',
};

/** 状态 → Tag 颜色 */
export const COURSE_STATUS_COLORS: Record<number, string> = {
  [COURSE_STATUS.DRAFT]: 'default',
  [COURSE_STATUS.PENDING]: 'processing',
  [COURSE_STATUS.APPROVED]: 'success',
  [COURSE_STATUS.REJECTED]: 'error',
};

/** 审核操作状态（对照 CourseAuditDTO.auditStatus） */
export const AUDIT_STATUS = {
  APPROVE: 1,
  REJECT: 2,
} as const;

/** 资源类型图标映射 */
export const RESOURCE_TYPE_MAP: Record<string, { label: string; icon: string }> = {
  video: { label: '视频', icon: 'PlayCircleOutlined' },
  document: { label: '文档', icon: 'FileTextOutlined' },
  pdf: { label: 'PDF', icon: 'FilePdfOutlined' },
  link: { label: '链接', icon: 'LinkOutlined' },
  image: { label: '图片', icon: 'PictureOutlined' },
};

/** 获取资源类型标签 */
export function getResourceTypeLabel(type: string | null): string {
  if (!type) return '其他';
  return RESOURCE_TYPE_MAP[type]?.label || '其他';
}

/** 获取资源类型颜色 */
export function getResourceTypeColor(type: string | null): string {
  if (!type) return 'default';
  const colorMap: Record<string, string> = {
    video: 'blue',
    document: 'green',
    pdf: 'orange',
    link: 'cyan',
    image: 'purple',
  };
  return colorMap[type] || 'default';
}
