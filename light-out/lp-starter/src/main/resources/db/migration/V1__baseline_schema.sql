-- ============================================================
-- 综合学习平台 - 完整MySQL建表脚本
-- 版本: 2.0
-- 字符集: utf8mb4 | 存储引擎: InnoDB
-- 总表数: 89
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- M1 用户与权限模块 (8张表) -- [v2.0 FIX: +admin_class, +student_class]
-- ============================================================

-- 1. 用户表
CREATE TABLE `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名/学号/工号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希(bcrypt)',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-封禁',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_status_del` (`status`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_user_dept_lookup` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 角色表
CREATE TABLE `role` (
  `role_id` INT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(32) NOT NULL COMMENT '角色名称: 超级管理员/分院管理员/任课教师/助教/学生',
  `role_code` VARCHAR(32) NOT NULL COMMENT '角色编码: super_admin/dept_admin/teacher/assistant/student',
  `level` TINYINT NOT NULL COMMENT '角色等级: 1-超级管理员 2-分院管理员 3-任课教师 4-助教 5-学生',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 3. 用户角色关联表
CREATE TABLE `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` INT NOT NULL COMMENT '角色ID',
  `scope_type` VARCHAR(32) NOT NULL DEFAULT 'global' COMMENT '权限范围类型: global-全校/department-分院/course-课程',
  `scope_id` BIGINT DEFAULT NULL COMMENT '权限范围ID: 分院ID或课程ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_scope` (`user_id`, `role_id`, `scope_type`, `scope_id`),
  KEY `idx_user_role_user` (`user_id`),
  KEY `idx_user_role_role` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 4. 分院表
CREATE TABLE `department` (
  `dept_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分院ID',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '分院名称',
  `parent_id` BIGINT DEFAULT NULL COMMENT '上级分院ID(支持多级层级)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  KEY `idx_dept_parent` (`parent_id`),
  CONSTRAINT `fk_department_parent` FOREIGN KEY (`parent_id`) REFERENCES `department` (`dept_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分院表';

-- 5. 用户分院关联表
CREATE TABLE `user_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `dept_id` BIGINT NOT NULL COMMENT '分院ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`, `dept_id`),
  KEY `idx_user_dept_dept` (`dept_id`),
  CONSTRAINT `fk_user_dept_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_dept_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户分院关联表';

-- 6. 用户通知偏好表
CREATE TABLE `user_notification_pref` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `channel` VARCHAR(32) NOT NULL COMMENT '通知渠道: in_app-站内/email-邮件/sms-短信',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-启用 0-禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_notif_pref` (`user_id`, `channel`),
  CONSTRAINT `fk_user_notif_pref_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知偏好表';

-- [v2.0 FIX] 行政班级表
CREATE TABLE `admin_class` (
  `class_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行政班级ID',
  `class_name` VARCHAR(64) NOT NULL COMMENT '班级名称(如计算机2301班)',
  `dept_id` BIGINT NOT NULL COMMENT '所属分院ID',
  `grade` VARCHAR(16) NOT NULL COMMENT '年级(如2023)',
  `counselor_id` BIGINT DEFAULT NULL COMMENT '辅导员ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`class_id`),
  KEY `idx_admin_class_dept` (`dept_id`),
  KEY `idx_admin_class_grade` (`grade`),
  CONSTRAINT `fk_admin_class_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_admin_class_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政班级表';

-- [v2.0 FIX] 学生-行政班级关联表
CREATE TABLE `student_class` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `class_id` BIGINT NOT NULL COMMENT '行政班级ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_class` (`student_id`, `class_id`),
  KEY `idx_student_class_class` (`class_id`),
  CONSTRAINT `fk_student_class_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_student_class_class` FOREIGN KEY (`class_id`) REFERENCES `admin_class` (`class_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生-行政班级关联表';


-- ============================================================
-- M2 课程与内容模块 (9张表)
-- ============================================================

-- 7. 课程表
CREATE TABLE `course` (
  `course_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` VARCHAR(128) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(64) DEFAULT NULL COMMENT '课程编码',
  `teacher_id` BIGINT NOT NULL COMMENT '创建教师ID',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属分院ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-待审核 2-已上线 3-已归档',
  `semester` VARCHAR(32) DEFAULT NULL COMMENT '学期: 如2025-2026-1',
  `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开课: 1-是 0-否',
  `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '课程封面URL',
  `description` TEXT DEFAULT NULL COMMENT '课程简介',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`course_id`),
  KEY `idx_course_teacher` (`teacher_id`),
  KEY `idx_course_dept` (`dept_id`),
  KEY `idx_course_status_del` (`status`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_course_semester_del` (`semester`, `deleted_at`), -- [v2.0 FIX]
  CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_course_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- 8. 课程审核记录表
CREATE TABLE `course_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `auditor_id` BIGINT NOT NULL COMMENT '审核人ID',
  `audit_status` TINYINT NOT NULL COMMENT '审核状态: 1-通过 2-驳回',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audited_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_audit_course` (`course_id`),
  KEY `idx_course_audit_auditor` (`auditor_id`),
  CONSTRAINT `fk_course_audit_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_course_audit_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程审核记录表';

-- 9. 课程-教师关联表
CREATE TABLE `course_teacher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
  `role` VARCHAR(32) NOT NULL DEFAULT 'lecturer' COMMENT '角色: lecturer-主讲/assistant-助教',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_teacher` (`course_id`, `teacher_id`, `role`),
  KEY `idx_course_teacher_teacher` (`teacher_id`),
  CONSTRAINT `fk_course_teacher_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_course_teacher_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程-教师关联表';

-- 10. 课程-学生关联表
CREATE TABLE `course_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `join_type` VARCHAR(32) NOT NULL DEFAULT 'required' COMMENT '加入方式: required-必修分配/elective-自主选课',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
  KEY `idx_course_student_student` (`student_id`),
  KEY `idx_course_student_join_type` (`join_type`),
  CONSTRAINT `fk_course_student_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_course_student_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程-学生关联表';

-- 11. 课程-班级关联表
CREATE TABLE `course_class` ( -- [v2.0 FIX] 增加行政班级关联
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `class_id` BIGINT DEFAULT NULL COMMENT '关联行政班级ID(批量选课用)', -- [v2.0 FIX]
  `class_name` VARCHAR(64) NOT NULL COMMENT '班级名称(课程内自定义分组名)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_class_course` (`course_id`),
  KEY `idx_course_class_admin` (`class_id`), -- [v2.0 FIX]
  UNIQUE KEY `uk_course_class_course_admin` (`course_id`, `class_id`), -- [v2.0 FIX]
  CONSTRAINT `fk_course_class_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_course_class_admin` FOREIGN KEY (`class_id`) REFERENCES `admin_class` (`class_id`) ON DELETE RESTRICT -- [v2.0 FIX]  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程-班级关联表';

-- 12. 章节表
CREATE TABLE `chapter` (
  `chapter_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `title` VARCHAR(255) NOT NULL COMMENT '章节标题',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号(支持拖拽调整)',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父章节ID(支持三级: 章→节→小节)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`chapter_id`),
  KEY `idx_chapter_course` (`course_id`),
  KEY `idx_chapter_parent` (`parent_id`),
  KEY `idx_chapter_sort` (`course_id`, `sort_order`),
  CONSTRAINT `fk_chapter_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_chapter_parent` FOREIGN KEY (`parent_id`) REFERENCES `chapter` (`chapter_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表(支持三级目录树)';

-- 13. 内容资源表
CREATE TABLE `content_resource` (
  `resource_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
  `resource_type` VARCHAR(32) NOT NULL COMMENT '资源类型: video-视频/graphic-图文/link-外链/pdf-PDF/ai_note-AI笔记链接',
  `title` VARCHAR(255) NOT NULL COMMENT '资源标题',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '资源URL',
  `duration` INT DEFAULT NULL COMMENT '视频时长(秒)',
  `content_text` LONGTEXT DEFAULT NULL COMMENT '图文内容(HTML)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`resource_id`),
  KEY `idx_resource_chapter` (`chapter_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_resource_sort` (`chapter_id`, `sort_order`),
  CONSTRAINT `fk_resource_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`chapter_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容资源表(多模态)';

-- 14. 个人素材库表
CREATE TABLE `material_library` (
  `material_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '素材ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID(教师)',
  `material_type` VARCHAR(32) NOT NULL COMMENT '素材类型: video/document/image/link',
  `title` VARCHAR(255) NOT NULL COMMENT '素材标题',
  `source_type` VARCHAR(32) NOT NULL DEFAULT 'upload' COMMENT '来源类型: upload-上传/ai_parsed-AI解析',
  `file_url` VARCHAR(1024) DEFAULT NULL COMMENT '文件URL',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '原始链接(AI解析来源)',
  `metadata` JSON DEFAULT NULL COMMENT '元数据(文件大小/格式等)',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`material_id`),
  KEY `idx_material_user_del` (`user_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_material_type_del` (`material_type`, `deleted_at`), -- [v2.0 FIX]
  CONSTRAINT `fk_material_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人素材库表(教师跨课复用)';

-- 15. 课程复制记录表
CREATE TABLE `course_copy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_course_id` BIGINT NOT NULL COMMENT '源课程ID',
  `target_course_id` BIGINT NOT NULL COMMENT '目标课程ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `copied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复制时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_copy_source` (`source_course_id`),
  KEY `idx_course_copy_target` (`target_course_id`),
  KEY `idx_course_copy_operator` (`operator_id`),
  CONSTRAINT `fk_course_copy_source` FOREIGN KEY (`source_course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `fk_course_copy_target` FOREIGN KEY (`target_course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `fk_course_copy_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程复制记录表';

-- ============================================================
-- M3 任务与考核模块 (12张表)
-- ============================================================

-- 16. 成绩构成定义表
CREATE TABLE `grade_scheme` (
  `scheme_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '构成ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `component_type` VARCHAR(32) NOT NULL COMMENT '构成类型: video-视频/assignment-作业/peer_review-互评/checkin-签到/exam-考试/experiment-实验',
  `weight` DECIMAL(5,2) NOT NULL COMMENT '权重百分比(如30.00)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号(支持拖拽调整)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`scheme_id`),
  KEY `idx_grade_scheme_course` (`course_id`),
  CONSTRAINT `fk_grade_scheme_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩构成定义表(课程成绩组成权重)';

-- 17. 任务表
CREATE TABLE `task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `chapter_id` BIGINT DEFAULT NULL COMMENT '章节ID(可选关联)',
  `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
  `description` TEXT DEFAULT NULL COMMENT '任务描述',
  `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型: assignment-作业/checkin-签到/quiz-测验/experiment-实验报告',
  `tag` VARCHAR(64) DEFAULT NULL COMMENT '任务标签: 课后作业/课堂测验/实验报告等',
  `weight` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '成绩权重',
  `publish_type` VARCHAR(32) NOT NULL DEFAULT 'immediate' COMMENT '发布方式: immediate-立即/scheduled-定时',
  `publish_time` DATETIME DEFAULT NULL COMMENT '定时发布时间',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `allow_late` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许迟交: 1-是 0-否',
  `late_penalty` DECIMAL(5,2) DEFAULT NULL COMMENT '迟交扣分比例(如10.00表示扣10%)',
  `max_score` DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '满分分值',
  `allow_resubmit` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许重交: 1-是 0-否',
  `attachment_urls` JSON DEFAULT NULL COMMENT '附件URL列表',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_task_course_del` (`course_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_task_chapter` (`chapter_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_task_deadline` (`deadline`),
  KEY `idx_task_course_type` (`course_id`, `task_type`),
  CONSTRAINT `fk_task_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_task_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`chapter_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 18. 题库表
CREATE TABLE `question_bank` (
  `q_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `q_type` VARCHAR(32) NOT NULL COMMENT '题型: single_choice-单选/multiple_choice-多选/judge-判断/fill_blank-填空/short_answer-简答',
  `content` TEXT NOT NULL COMMENT '题干内容',
  `options` JSON DEFAULT NULL COMMENT '选项(JSON数组, 选择题用)',
  `answer` TEXT NOT NULL COMMENT '参考答案',
  `analysis` TEXT DEFAULT NULL COMMENT '解析',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '难度: 1-简单 2-中等 3-困难',
  `tags` JSON DEFAULT NULL COMMENT '标签列表(JSON数组)',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源: manual-手动/batch_import-批量导入/ai_generated-AI生成',
  `import_id` BIGINT DEFAULT NULL COMMENT '导入批次ID(批量导入时关联)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`q_id`),
  KEY `idx_question_course` (`course_id`),
  KEY `idx_question_creator` (`creator_id`),
  KEY `idx_question_type` (`q_type`),
  KEY `idx_question_difficulty` (`difficulty`),
  KEY `idx_question_course_type` (`course_id`, `q_type`),
  CONSTRAINT `fk_question_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_question_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- 19. 题目导入记录表
CREATE TABLE `question_import` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `file_url` VARCHAR(1024) NOT NULL COMMENT '导入文件URL',
  `file_format` VARCHAR(32) NOT NULL COMMENT '文件格式: excel/word',
  `import_count` INT NOT NULL DEFAULT 0 COMMENT '导入题目总数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功导入数',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `error_detail` JSON DEFAULT NULL COMMENT '错误详情(行号+原因)',
  `status` VARCHAR(32) NOT NULL DEFAULT 'processing' COMMENT '状态: processing-处理中/completed-完成/failed-失败',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_qimport_course` (`course_id`),
  KEY `idx_qimport_operator` (`operator_id`),
  CONSTRAINT `fk_qimport_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_qimport_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目导入记录表';

-- 20. 试卷表
CREATE TABLE `exam_paper` (
  `paper_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
  `title` VARCHAR(255) NOT NULL COMMENT '试卷标题',
  `total_score` DECIMAL(5,2) NOT NULL COMMENT '总分',
  `duration` INT DEFAULT NULL COMMENT '限时(分钟)',
  `shuffle_questions` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否乱序出题: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`paper_id`),
  KEY `idx_paper_course` (`course_id`),
  KEY `idx_paper_task` (`task_id`),
  CONSTRAINT `fk_paper_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_paper_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- 21. 试卷-题目关联表
CREATE TABLE `paper_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
  `q_id` BIGINT NOT NULL COMMENT '题目ID',
  `score` DECIMAL(5,2) NOT NULL COMMENT '该题分值',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`, `q_id`),
  KEY `idx_paper_question_q` (`q_id`),
  CONSTRAINT `fk_paper_question_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`paper_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_paper_question_q` FOREIGN KEY (`q_id`) REFERENCES `question_bank` (`q_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷-题目关联表';

-- 22. 学生提交表
CREATE TABLE `submission` (
  `submission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `submit_type` VARCHAR(32) NOT NULL COMMENT '提交类型: assignment-作业/quiz-测验/checkin-签到',
  `content` JSON DEFAULT NULL COMMENT '提交内容(文本/文件URL等)',
  `attachment_urls` JSON DEFAULT NULL COMMENT '附件URL列表',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `is_late` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否迟交: 1-是 0-否',
  `is_excellent` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否优秀范本: 1-是 0-否',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待批阅/graded-已批阅',
  `teacher_comment` TEXT DEFAULT NULL COMMENT '教师评语',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '教师评分',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`submission_id`),
  KEY `idx_submission_task` (`task_id`),
  KEY `idx_submission_student` (`student_id`),
  KEY `idx_submission_status` (`status`),
  KEY `idx_submission_task_student` (`task_id`, `student_id`),
  CONSTRAINT `fk_submission_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_submission_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生提交表';

-- 23. 测验作答记录表
CREATE TABLE `exam_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id` BIGINT NOT NULL COMMENT '提交ID',
  `q_id` BIGINT NOT NULL COMMENT '题目ID',
  `student_answer` TEXT DEFAULT NULL COMMENT '学生答案',
  `is_correct` TINYINT(1) DEFAULT NULL COMMENT '是否正确: 1-是 0-否',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '该题得分',
  `answered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_exam_answer_submission` (`submission_id`),
  KEY `idx_exam_answer_q` (`q_id`),
  KEY `idx_exam_answer_submission_q` (`submission_id`, `q_id`),
  CONSTRAINT `fk_exam_answer_submission` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`submission_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_exam_answer_q` FOREIGN KEY (`q_id`) REFERENCES `question_bank` (`q_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测验作答记录表(逐题详情)';

-- 24. 成绩表
CREATE TABLE `grade` (
  `grade_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `submission_id` BIGINT DEFAULT NULL COMMENT '提交ID',
  `score` DECIMAL(5,2) NOT NULL COMMENT '原始得分',
  `weighted_score` DECIMAL(5,2) DEFAULT NULL COMMENT '加权得分',
  `component_type` VARCHAR(32) DEFAULT NULL COMMENT '成绩类型: video/assignment/peer_review等',
  `is_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '教师是否确认: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`grade_id`),
  KEY `idx_grade_course` (`course_id`),
  KEY `idx_grade_student` (`student_id`),
  KEY `idx_grade_task` (`task_id`),
  KEY `idx_grade_course_student` (`course_id`, `student_id`),
  KEY `idx_grade_submission` (`submission_id`),
  CONSTRAINT `fk_grade_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_grade_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_grade_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_grade_submission` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`submission_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表(单项成绩)';

-- 25. 成绩汇总表
CREATE TABLE `grade_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `final_score` DECIMAL(5,2) NOT NULL COMMENT '加权总评成绩',
  `rank_in_course` INT DEFAULT NULL COMMENT '课程内排名',
  `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认教师ID',
  `confirmed_at` DATETIME DEFAULT NULL COMMENT '确认时间',
  `is_exported` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已导出: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_summary` (`course_id`, `student_id`),
  KEY `idx_grade_summary_student` (`student_id`),
  KEY `idx_grade_summary_confirmed` (`confirmed_by`),
  CONSTRAINT `fk_grade_summary_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_grade_summary_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_grade_summary_confirmer` FOREIGN KEY (`confirmed_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩汇总表(加权总评)';

-- 26. 成绩申诉表
CREATE TABLE `grade_appeal` (
  `appeal_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
  `grade_id` BIGINT NOT NULL COMMENT '成绩ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `appeal_reason` TEXT NOT NULL COMMENT '申诉理由',
  `teacher_response` TEXT DEFAULT NULL COMMENT '教师回复',
  `new_score` DECIMAL(5,2) DEFAULT NULL COMMENT '修改后成绩',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待处理/modified-已修改/rejected-驳回',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appeal_id`),
  KEY `idx_appeal_grade` (`grade_id`),
  KEY `idx_appeal_student` (`student_id`),
  KEY `idx_appeal_status` (`status`),
  CONSTRAINT `fk_appeal_grade` FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_appeal_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩申诉表(全程留痕)';

-- 27. 防作弊记录表
CREATE TABLE `cheat_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id` BIGINT NOT NULL COMMENT '提交ID',
  `cheat_type` VARCHAR(32) NOT NULL COMMENT '作弊类型: tab_switch-切屏/copy_paste-复制粘贴/right_click-右键',
  `detail` VARCHAR(512) DEFAULT NULL COMMENT '详细描述(如切屏次数、复制内容等)',
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cheat_submission` (`submission_id`),
  KEY `idx_cheat_type` (`cheat_type`),
  KEY `idx_cheat_occurred` (`occurred_at`),
  CONSTRAINT `fk_cheat_submission` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`submission_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='防作弊记录表';

-- ============================================================
-- M4 互评系统模块 (4张表)
-- ============================================================

-- 28. 互评配置表
CREATE TABLE `peer_review_config` (
  `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `reviewer_count` INT NOT NULL DEFAULT 3 COMMENT '每人互评数量(3-5)',
  `rubric` JSON NOT NULL COMMENT '评分量表(JSON结构, 含维度/分值/说明)',
  `is_anonymous` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否匿名: 1-是 0-否',
  `start_time` DATETIME NOT NULL COMMENT '互评开始时间',
  `end_time` DATETIME NOT NULL COMMENT '互评截止时间',
  `weight_in_grade` DECIMAL(5,2) DEFAULT NULL COMMENT '互评成绩占比(%)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_peer_review_config_task` (`task_id`),
  CONSTRAINT `fk_peer_config_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互评配置表';

-- 29. 互评分配表
CREATE TABLE `peer_review_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` BIGINT NOT NULL COMMENT '配置ID',
  `submission_id` BIGINT NOT NULL COMMENT '被评作业提交ID',
  `reviewer_id` BIGINT NOT NULL COMMENT '评阅人ID(学生)',
  `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `is_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成评阅: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_peer_assignment` (`config_id`, `submission_id`, `reviewer_id`),
  KEY `idx_peer_assignment_config` (`config_id`),
  KEY `idx_peer_assignment_submission` (`submission_id`),
  KEY `idx_peer_assignment_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_peer_assignment_config` FOREIGN KEY (`config_id`) REFERENCES `peer_review_config` (`config_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_peer_assignment_submission` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`submission_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_peer_assignment_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互评分配表(系统随机分配)';

-- 30. 互评评分表
CREATE TABLE `peer_review_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `assignment_id` BIGINT NOT NULL COMMENT '互评分配ID',
  `rubric_item_id` VARCHAR(64) NOT NULL COMMENT '评分量表维度ID',
  `score` DECIMAL(5,2) NOT NULL COMMENT '该维度得分',
  `comment` TEXT DEFAULT NULL COMMENT '评语',
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_peer_score_assignment` (`assignment_id`),
  KEY `idx_peer_score_rubric` (`assignment_id`, `rubric_item_id`),
  CONSTRAINT `fk_peer_score_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `peer_review_assignment` (`id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互评评分表(按量表维度逐项打分)';

-- 31. 互评申诉表
CREATE TABLE `peer_review_appeal` (
  `appeal_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
  `assignment_id` BIGINT NOT NULL COMMENT '互评分配ID',
  `appellant_id` BIGINT NOT NULL COMMENT '申诉人ID',
  `reason` TEXT NOT NULL COMMENT '申诉理由',
  `teacher_response` TEXT DEFAULT NULL COMMENT '教师回复',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待处理/approved-已通过/rejected-驳回',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appeal_id`),
  KEY `idx_peer_appeal_assignment` (`assignment_id`),
  KEY `idx_peer_appeal_appellant` (`appellant_id`),
  CONSTRAINT `fk_peer_appeal_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `peer_review_assignment` (`id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_peer_appeal_appellant` FOREIGN KEY (`appellant_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互评申诉表';

-- ============================================================
-- M5 学习体验与进度模块 (6张表)
-- ============================================================

-- 32. 视频观看记录表
CREATE TABLE `video_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID(视频)',
  `last_position` INT NOT NULL DEFAULT 0 COMMENT '上次播放位置(秒)',
  `watch_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '观看百分比(0-100)',
  `duration_watched` INT NOT NULL DEFAULT 0 COMMENT '累计观看时长(秒)',
  `play_count` INT NOT NULL DEFAULT 0 COMMENT '播放次数',
  `last_watched_at` DATETIME DEFAULT NULL COMMENT '最后观看时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_progress` (`user_id`, `resource_id`),
  KEY `idx_video_progress_user` (`user_id`),
  KEY `idx_video_progress_resource` (`resource_id`),
  CONSTRAINT `fk_video_progress_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_video_progress_resource` FOREIGN KEY (`resource_id`) REFERENCES `content_resource` (`resource_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频观看记录表(断点续学)';

-- 33. 文档阅读记录表
CREATE TABLE `doc_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID(文档)',
  `last_position` INT NOT NULL DEFAULT 0 COMMENT '上次阅读位置(字符偏移/页码)',
  `read_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '阅读百分比(0-100)',
  `last_read_at` DATETIME DEFAULT NULL COMMENT '最后阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_progress` (`user_id`, `resource_id`),
  KEY `idx_doc_progress_user` (`user_id`),
  KEY `idx_doc_progress_resource` (`resource_id`),
  CONSTRAINT `fk_doc_progress_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_doc_progress_resource` FOREIGN KEY (`resource_id`) REFERENCES `content_resource` (`resource_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档阅读记录表(断点续读)';

-- 34. 视频管控配置表
CREATE TABLE `video_control` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID(视频)',
  `no_drag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '禁止拖拽: 1-是 0-否',
  `no_fast_forward` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '禁止快进: 1-是 0-否',
  `min_play_speed` DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '最低播放倍速(如0.50)',
  `max_play_speed` DECIMAL(3,2) NOT NULL DEFAULT 2.00 COMMENT '最高播放倍速(如2.00)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_control_resource` (`resource_id`),
  CONSTRAINT `fk_video_control_resource` FOREIGN KEY (`resource_id`) REFERENCES `content_resource` (`resource_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频管控配置表(防拖拽/防快进)';

-- 35. 学习时长统计表
CREATE TABLE `study_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `video_duration` INT NOT NULL DEFAULT 0 COMMENT '视频学习时长(秒)',
  `doc_duration` INT NOT NULL DEFAULT 0 COMMENT '文档学习时长(秒)',
  `total_duration` INT NOT NULL DEFAULT 0 COMMENT '总学习时长(秒)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_stats` (`user_id`, `course_id`, `stat_date`),
  KEY `idx_study_stats_user` (`user_id`),
  KEY `idx_study_stats_course` (`course_id`),
  KEY `idx_study_stats_date` (`stat_date`),
  CONSTRAINT `fk_study_stats_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_study_stats_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习时长统计表(按天/课程汇总)';

-- 36. 个人学习报告表
CREATE TABLE `learning_report` (
  `report_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `course_id` BIGINT DEFAULT NULL COMMENT '课程ID(课程报告时有值)',
  `report_type` VARCHAR(32) NOT NULL COMMENT '报告类型: course-课程报告/self_study-自主报告',
  `period` VARCHAR(32) NOT NULL COMMENT '统计周期: weekly-周/monthly-月',
  `data` JSON NOT NULL COMMENT '报告数据(学习时长/完成进度/正确率等)',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`report_id`),
  KEY `idx_report_user` (`user_id`),
  KEY `idx_report_course` (`course_id`),
  KEY `idx_report_type_period` (`report_type`, `period`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_report_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人学习报告表';

-- 37. AI即时问答记录表
CREATE TABLE `ai_qa_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID(视频/文档)',
  `question` TEXT NOT NULL COMMENT '用户提问',
  `answer` TEXT NOT NULL COMMENT 'AI回答',
  `source_refs` JSON DEFAULT NULL COMMENT '引用来源(段落/时间戳列表)',
  `asked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_qa_user` (`user_id`),
  KEY `idx_ai_qa_resource` (`resource_id`),
  KEY `idx_ai_qa_asked_at` (`asked_at`),
  CONSTRAINT `fk_ai_qa_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_ai_qa_resource` FOREIGN KEY (`resource_id`) REFERENCES `content_resource` (`resource_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI即时问答记录表(边看边问)';

-- ============================================================
-- M6 消息通知与待办模块 (7张表)
-- ============================================================

-- 38. 课程消息表(群聊)
CREATE TABLE `course_chat` (
  `msg_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `msg_type` VARCHAR(32) NOT NULL DEFAULT 'text' COMMENT '消息类型: text-文本/image-图片/file-文件',
  `file_url` VARCHAR(1024) DEFAULT NULL COMMENT '文件URL(文件/图片消息)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_course_chat_course` (`course_id`),
  KEY `idx_course_chat_sender` (`sender_id`),
  KEY `idx_course_chat_created` (`course_id`, `created_at`),
  CONSTRAINT `fk_course_chat_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_course_chat_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程消息表(按课程群聊)';

-- 39. 私信会话表
CREATE TABLE `conversation` (
  `conversation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `participants` JSON NOT NULL COMMENT '参与人ID列表(JSON数组)',
  `conv_type` VARCHAR(32) NOT NULL DEFAULT 'private' COMMENT '会话类型: private-一对一/group-小组',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';

-- 40. 私信消息表
CREATE TABLE `message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `msg_type` VARCHAR(32) NOT NULL DEFAULT 'text' COMMENT '消息类型: text-文本/image-图片/file-文件',
  `file_url` VARCHAR(1024) DEFAULT NULL COMMENT '文件URL',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_message_conversation` (`conversation_id`),
  KEY `idx_message_sender` (`sender_id`),
  KEY `idx_message_conv_created` (`conversation_id`, `created_at`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`conversation_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

-- 41. 通知表
CREATE TABLE `notification` (
  `notify_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: global-全校/department-分院/course-课程/personal-个人',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID(分院ID/课程ID/用户ID)',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT DEFAULT NULL COMMENT '通知内容',
  `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
  `notify_type` VARCHAR(32) NOT NULL COMMENT '通知类型: course_notice-课程通知/assignment_reminder-作业提醒/exam_reminder-考试提醒/system-系统通知',
  `is_batch` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否批量通知: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notify_id`),
  KEY `idx_notification_target` (`target_type`, `target_id`),
  KEY `idx_notification_type` (`notify_type`),
  KEY `idx_notification_sender` (`sender_id`),
  CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 42. 用户通知状态表
CREATE TABLE `user_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_id` BIGINT NOT NULL COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读: 1-是 0-否',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_notification` (`notify_id`, `user_id`),
  KEY `idx_user_notif_user` (`user_id`),
  KEY `idx_user_notif_read` (`user_id`, `is_read`),
  CONSTRAINT `fk_user_notif_notify` FOREIGN KEY (`notify_id`) REFERENCES `notification` (`notify_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_user_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知状态表(已读/未读)';

-- 43. 待办清单表
CREATE TABLE `todo` (
  `todo_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型: assignment-作业/video-视频/peer_review-互评/cet-四六级背诵',
  `source_id` BIGINT NOT NULL COMMENT '来源ID(任务ID/资源ID等)',
  `title` VARCHAR(255) NOT NULL COMMENT '待办标题',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '待办描述',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `tag` VARCHAR(64) DEFAULT NULL COMMENT '标签(按标签筛选)',
  `is_done` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成: 1-是 0-否',
  `done_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`todo_id`),
  KEY `idx_todo_user` (`user_id`),
  KEY `idx_todo_source` (`source_type`, `source_id`),
  KEY `idx_todo_user_status` (`user_id`, `is_done`),
  KEY `idx_todo_deadline` (`deadline`),
  CONSTRAINT `fk_todo_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办清单表(统一待办体系)';

-- 44. 系统公告表
CREATE TABLE `system_announcement` (
  `ann_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(255) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` BIGINT NOT NULL COMMENT '发布人ID',
  `publish_time` DATETIME NOT NULL COMMENT '发布时间',
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶: 1-是 0-否',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否生效: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ann_id`),
  KEY `idx_announcement_publish_time` (`publish_time`),
  KEY `idx_announcement_pinned` (`is_pinned`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ============================================================
-- M7 社区交流与资源共享模块 (7张表)
-- ============================================================

-- 45. 社区板块表
CREATE TABLE `board` (
  `board_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '板块ID',
  `board_type` VARCHAR(32) NOT NULL COMMENT '板块类型: course_qa-课程答疑/general-综合讨论/water-灌水区/teaching_share-教学分享/resource_expand-资源拓展',
  `name` VARCHAR(128) NOT NULL COMMENT '板块名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '板块描述',
  `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(课程答疑区)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`board_id`),
  KEY `idx_board_type` (`board_type`),
  KEY `idx_board_course` (`course_id`),
  CONSTRAINT `fk_board_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区板块表';

-- 46. 帖子表
CREATE TABLE `post` (
  `post_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `board_id` BIGINT NOT NULL COMMENT '板块ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `title` VARCHAR(255) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `post_type` VARCHAR(32) NOT NULL DEFAULT 'discussion' COMMENT '帖子类型: discussion-讨论/resource_share-资源分享/ai_note_share-AI笔记分享',
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶: 1-是 0-否',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数(冗余计数)',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数(冗余计数)',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回帖数(冗余计数)',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`post_id`),
  KEY `idx_post_board_del` (`board_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_post_author` (`author_id`),
  KEY `idx_post_type` (`post_type`),
  KEY `idx_post_created` (`created_at`),
  KEY `idx_post_board_created` (`board_id`, `created_at`),
  CONSTRAINT `fk_post_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`board_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 47. 回帖表
CREATE TABLE `reply` (
  `reply_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回帖ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `content` TEXT NOT NULL COMMENT '回帖内容',
  `parent_reply_id` BIGINT DEFAULT NULL COMMENT '父回帖ID(支持楼中楼)',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_teacher_liked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '教师是否点赞: 1-是 0-否(教师点赞自动置顶)',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`reply_id`),
  KEY `idx_reply_post_del` (`post_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_reply_author` (`author_id`),
  KEY `idx_reply_parent` (`parent_reply_id`),
  KEY `idx_reply_post_created` (`post_id`, `created_at`),
  CONSTRAINT `fk_reply_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_reply_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_reply_parent` FOREIGN KEY (`parent_reply_id`) REFERENCES `reply` (`reply_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回帖表(支持楼中楼)';

-- 48. 点赞记录表
CREATE TABLE `like_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: post-帖子/reply-回帖',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_record` (`user_id`, `target_type`, `target_id`),
  KEY `idx_like_target` (`target_type`, `target_id`),
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录表';

-- 49. 收藏表
CREATE TABLE `collection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: post-帖子/resource-社区资源/ai_note-AI笔记',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection` (`user_id`, `target_type`, `target_id`),
  KEY `idx_collection_target` (`target_type`, `target_id`),
  CONSTRAINT `fk_collection_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 50. 社区资源表
CREATE TABLE `community_resource` (
  `res_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `uploader_id` BIGINT NOT NULL COMMENT '上传人ID',
  `resource_type` VARCHAR(32) NOT NULL COMMENT '资源类型: note-笔记/review_material-复习资料/literature-文献/teaching_experience-教学经验',
  `title` VARCHAR(255) NOT NULL COMMENT '资源标题',
  `file_url` VARCHAR(1024) DEFAULT NULL COMMENT '文件URL',
  `description` TEXT DEFAULT NULL COMMENT '资源描述',
  `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(可选)',
  `download_count` INT NOT NULL DEFAULT 0 COMMENT '下载数',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`res_id`),
  KEY `idx_community_res_uploader_del` (`uploader_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_community_res_type` (`resource_type`),
  KEY `idx_community_res_course` (`course_id`),
  CONSTRAINT `fk_community_res_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_community_res_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区资源表';

-- 51. 投诉反馈表
CREATE TABLE `report` (
  `report_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投诉ID',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: post-帖子/reply-回帖/resource-资源/user-用户',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `reason` TEXT NOT NULL COMMENT '举报理由',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待处理/processed-已处理/dismissed-驳回',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `result` VARCHAR(512) DEFAULT NULL COMMENT '处理结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`report_id`),
  KEY `idx_report_reporter` (`reporter_id`),
  KEY `idx_report_target` (`target_type`, `target_id`),
  KEY `idx_report_status` (`status`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_report_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉反馈表';

-- ============================================================
-- M8 自主学习模块 (3张表)
-- ============================================================

-- 52. 自主选课记录表
CREATE TABLE `self_study_enrollment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `study_mode` VARCHAR(32) NOT NULL DEFAULT 'video_exercise' COMMENT '学习模式: video_exercise-视频+习题/knowledge_review-知识点+错题/group_collaboration-小组协作/ai_deep_reading-AI笔记深度阅读',
  `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否活跃: 1-是 0-否',
  `progress` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '学习进度(0-100)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_study_enrollment` (`user_id`, `course_id`),
  KEY `idx_self_study_user` (`user_id`),
  KEY `idx_self_study_course` (`course_id`),
  CONSTRAINT `fk_self_study_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_self_study_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自主选课记录表(不纳入学分)';

-- 53. 错题本表
CREATE TABLE `error_book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `q_id` BIGINT NOT NULL COMMENT '题目ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型: course-课程练习/cet-四六级/self_study-自主练习',
  `wrong_answer` TEXT DEFAULT NULL COMMENT '错误答案',
  `correct_answer` TEXT DEFAULT NULL COMMENT '正确答案',
  `wrong_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数',
  `last_reviewed_at` DATETIME DEFAULT NULL COMMENT '最后复习时间',
  `mastery_level` TINYINT NOT NULL DEFAULT 0 COMMENT '掌握程度: 0-未掌握 1-部分掌握 2-已掌握',
  `note` TEXT DEFAULT NULL COMMENT '个人笔记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_error_book` (`user_id`, `q_id`, `source_type`),
  KEY `idx_error_book_user` (`user_id`),
  KEY `idx_error_book_q` (`q_id`),
  KEY `idx_error_book_mastery` (`user_id`, `mastery_level`),
  CONSTRAINT `fk_error_book_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_error_book_q` FOREIGN KEY (`q_id`) REFERENCES `question_bank` (`q_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本表(自动归集)';

-- 54. 学习模式推荐表
CREATE TABLE `study_mode_recommend` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `recommended_mode` VARCHAR(32) NOT NULL COMMENT '推荐模式: video_exercise/knowledge_review/group_collaboration/ai_deep_reading',
  `reason` TEXT DEFAULT NULL COMMENT '推荐理由',
  `confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '推荐置信度(0-1)',
  `recommended_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
  `is_adopted` TINYINT(1) DEFAULT NULL COMMENT '用户是否采纳: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_study_recommend_user` (`user_id`),
  KEY `idx_study_recommend_mode` (`recommended_mode`),
  CONSTRAINT `fk_study_recommend_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习模式推荐表';

-- ============================================================
-- M9 四六级专项模块 (10张表) -- [v2.0 FIX: +cet_question_bank, cet_practice改]
-- ============================================================

-- 55. 词书表
CREATE TABLE `vocab_book` (
  `book_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '词书ID',
  `book_name` VARCHAR(128) NOT NULL COMMENT '词书名称',
  `level` VARCHAR(16) NOT NULL COMMENT '等级: cet4-四级/cet6-六级',
  `word_count` INT NOT NULL DEFAULT 0 COMMENT '词数',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '词书描述',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`book_id`),
  KEY `idx_vocab_book_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词书表(四六级词库)';

-- 56. 单词表
CREATE TABLE `vocab_word` (
  `word_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '单词ID',
  `book_id` BIGINT NOT NULL COMMENT '词书ID',
  `word` VARCHAR(128) NOT NULL COMMENT '英文单词',
  `phonetic` VARCHAR(128) DEFAULT NULL COMMENT '音标',
  `meaning` VARCHAR(512) NOT NULL COMMENT '中文释义',
  `example_sentence` TEXT DEFAULT NULL COMMENT '例句',
  `audio_url` VARCHAR(512) DEFAULT NULL COMMENT '发音音频URL',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`word_id`),
  KEY `idx_vocab_word_book` (`book_id`),
  KEY `idx_vocab_word_word` (`word`),
  KEY `idx_vocab_word_book_sort` (`book_id`, `sort_order`),
  CONSTRAINT `fk_vocab_word_book` FOREIGN KEY (`book_id`) REFERENCES `vocab_book` (`book_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';

-- 57. 背诵计划表
CREATE TABLE `recite_plan` (
  `plan_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '词书ID',
  `daily_new` INT NOT NULL DEFAULT 20 COMMENT '每日新学词数',
  `daily_review` INT NOT NULL DEFAULT 40 COMMENT '每日复习词数',
  `plan_type` VARCHAR(32) NOT NULL DEFAULT 'ebbinghaus' COMMENT '计划类型: ebbinghaus-艾宾浩斯/custom-自定义',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '预计结束日期',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态: active-进行中/paused-暂停/completed-已结束',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`plan_id`),
  KEY `idx_recite_plan_user` (`user_id`),
  KEY `idx_recite_plan_book` (`book_id`),
  KEY `idx_recite_plan_status` (`status`),
  CONSTRAINT `fk_recite_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_recite_plan_book` FOREIGN KEY (`book_id`) REFERENCES `vocab_book` (`book_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背诵计划表(艾宾浩斯记忆法)';

-- 58. 背诵记录表
CREATE TABLE `recite_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id` BIGINT NOT NULL COMMENT '计划ID',
  `word_id` BIGINT NOT NULL COMMENT '单词ID',
  `result` VARCHAR(16) NOT NULL COMMENT '记忆结果: known-认识/fuzzy-模糊/unknown-不认识',
  `review_round` INT NOT NULL DEFAULT 1 COMMENT '复习轮次',
  `next_review_date` DATE DEFAULT NULL COMMENT '下次复习日期(艾宾浩斯间隔)',
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recite_record_plan` (`plan_id`),
  KEY `idx_recite_record_word` (`word_id`),
  KEY `idx_recite_record_next_review` (`next_review_date`),
  KEY `idx_recite_record_plan_word` (`plan_id`, `word_id`),
  CONSTRAINT `fk_recite_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `recite_plan` (`plan_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_recite_record_word` FOREIGN KEY (`word_id`) REFERENCES `vocab_word` (`word_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背诵记录表(记忆跟踪)';

-- 59. 闯关记录表
CREATE TABLE `challenge_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `challenge_type` VARCHAR(32) NOT NULL COMMENT '闯关类型: word_spell-单词拼写/word_match-词义匹配/listen_choice-听力选择',
  `level` INT NOT NULL DEFAULT 1 COMMENT '关卡等级',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `passed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过: 1-是 0-否',
  `reward_points` INT NOT NULL DEFAULT 0 COMMENT '获得积分',
  `time_spent` INT DEFAULT NULL COMMENT '用时(秒)',
  `completed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_challenge_user` (`user_id`),
  KEY `idx_challenge_type_level` (`challenge_type`, `level`),
  CONSTRAINT `fk_challenge_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关记录表(趣味闯关+奖励)';

-- 60. 四六级专项练习表
CREATE TABLE `cet_practice` ( -- [v2.0 FIX] 新增cet_q_id关联四六级题库
  `practice_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '练习ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `practice_type` VARCHAR(32) NOT NULL COMMENT '练习类型: listening-听力/reading-阅读/writing-写作/translation-翻译',
  `cet_q_id` BIGINT DEFAULT NULL COMMENT '关联四六级题库ID(单题练习时)', -- [v2.0 FIX]
  `questions` JSON DEFAULT NULL COMMENT '题目列表(JSON, 整套试卷模式时使用)', -- [v2.0 FIX] 改为可选
  `user_answers` JSON DEFAULT NULL COMMENT '用户作答(JSON)',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
  `total_score` DECIMAL(5,2) NOT NULL COMMENT '总分',
  `time_spent` INT DEFAULT NULL COMMENT '用时(秒)',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`practice_id`),
  KEY `idx_cet_practice_user` (`user_id`),
  KEY `idx_cet_practice_type` (`practice_type`),
  KEY `idx_cet_practice_cet_q` (`cet_q_id`), -- [v2.0 FIX]
  CONSTRAINT `fk_cet_practice_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT, -- [v2.0 FIX]
  CONSTRAINT `fk_cet_practice_cet_q` FOREIGN KEY (`cet_q_id`) REFERENCES `cet_question_bank` (`cet_q_id`) ON DELETE RESTRICT -- [v2.0 FIX] ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='四六级专项练习表';

-- 61. 四六级模拟考试表
CREATE TABLE `cet_mock_exam` (
  `exam_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模拟考试ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `level` VARCHAR(16) NOT NULL COMMENT '等级: cet4-四级/cet6-六级',
  `paper_url` VARCHAR(1024) DEFAULT NULL COMMENT '试卷URL',
  `total_score` DECIMAL(5,2) NOT NULL COMMENT '总分',
  `listening_score` DECIMAL(5,2) DEFAULT NULL COMMENT '听力得分',
  `reading_score` DECIMAL(5,2) DEFAULT NULL COMMENT '阅读得分',
  `writing_score` DECIMAL(5,2) DEFAULT NULL COMMENT '写作得分',
  `translation_score` DECIMAL(5,2) DEFAULT NULL COMMENT '翻译得分',
  `time_spent` INT DEFAULT NULL COMMENT '用时(秒)',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`exam_id`),
  KEY `idx_cet_mock_user` (`user_id`),
  KEY `idx_cet_mock_level` (`level`),
  CONSTRAINT `fk_cet_mock_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='四六级模拟考试表';

-- 62. 备考报告表
CREATE TABLE `cet_report` (
  `report_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `level` VARCHAR(16) NOT NULL COMMENT '等级: cet4-四级/cet6-六级',
  `data` JSON NOT NULL COMMENT '备考数据(各维度得分/进度/错题分布等)',
  `recommended_resources` JSON DEFAULT NULL COMMENT '推荐资源列表',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`report_id`),
  KEY `idx_cet_report_user` (`user_id`),
  KEY `idx_cet_report_level` (`level`),
  CONSTRAINT `fk_cet_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备考报告表';

-- [v2.0 FIX] 四六级题库表
CREATE TABLE `cet_question_bank` (
  `cet_q_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '四六级题目ID',
  `level` VARCHAR(8) NOT NULL COMMENT '等级: cet4-四级/cet6-六级',
  `section` VARCHAR(16) NOT NULL COMMENT '题型: listening-听力/reading-阅读/writing-写作/translation-翻译',
  `content` TEXT NOT NULL COMMENT '题干内容',
  `options` JSON DEFAULT NULL COMMENT '选项(JSON, 选择题使用)',
  `answer` TEXT NOT NULL COMMENT '参考答案',
  `analysis` TEXT DEFAULT NULL COMMENT '解析',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '难度: 1-简单 2-中等 3-困难',
  `tags` JSON DEFAULT NULL COMMENT '标签(如主题/考点)',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源: manual-手动/AI-AI生成/import-批量导入',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cet_q_id`),
  KEY `idx_cet_q_level_section` (`level`, `section`),
  KEY `idx_cet_q_difficulty` (`difficulty`),
  KEY `idx_cet_q_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='四六级题库表';

-- ============================================================
-- M10 AI智能笔记引擎模块 (14张表)
-- ============================================================

-- 63. 笔记本表
CREATE TABLE `notebook` (
  `notebook_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记本ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '笔记本标题',
  `description` TEXT DEFAULT NULL COMMENT '笔记本描述',
  `visibility` VARCHAR(32) NOT NULL DEFAULT 'private' COMMENT '可见性: public-公开/course_inner-课程内/private-私有',
  `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(可选)',
  `source_count` INT NOT NULL DEFAULT 0 COMMENT '来源数量(冗余)',
  `chat_count` INT NOT NULL DEFAULT 0 COMMENT '对话数量(冗余)',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notebook_id`),
  KEY `idx_notebook_user_del` (`user_id`, `deleted_at`), -- [v2.0 FIX]
  KEY `idx_notebook_course` (`course_id`),
  KEY `idx_notebook_visibility` (`visibility`),
  CONSTRAINT `fk_notebook_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_notebook_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记本表(学习主题容器)';

-- 64. 笔记本来源表
CREATE TABLE `notebook_source` (
  `source_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '来源ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型: platform_video-平台视频/youtube-YouTube/bilibili-B站/pdf-PDF/word-Word/ppt-PPT/webpage-网页/doi-学术论文/doi_audio-音频',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL',
  `file_path` VARCHAR(1024) DEFAULT NULL COMMENT '上传文件路径',
  `title` VARCHAR(255) DEFAULT NULL COMMENT '来源标题',
  `parsed_content` LONGTEXT DEFAULT NULL COMMENT 'AI解析后文本内容',
  `embedding_id` VARCHAR(128) DEFAULT NULL COMMENT '向量数据库中的ID(用于RAG检索)',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '解析状态: pending-待解析/parsing-解析中/completed-完成/failed-失败',
  `error_message` VARCHAR(512) DEFAULT NULL COMMENT '解析失败原因',
  `added_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`source_id`),
  KEY `idx_nb_source_notebook` (`notebook_id`),
  KEY `idx_nb_source_type` (`source_type`),
  KEY `idx_nb_source_status` (`status`),
  CONSTRAINT `fk_nb_source_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记本来源表(多源聚合)';

-- 65. AI生成摘要表
CREATE TABLE `note_summary` (
  `summary_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '摘要ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `source_id` BIGINT NOT NULL COMMENT '来源ID',
  `summary_type` VARCHAR(32) NOT NULL COMMENT '摘要类型: structured-结构化摘要/timeline-时间轴索引/glossary-术语表/faq-FAQ',
  `content` JSON NOT NULL COMMENT '摘要内容(JSON结构, 按类型不同结构不同)',
  `model_used` VARCHAR(64) DEFAULT NULL COMMENT '使用的AI模型',
  `token_count` INT DEFAULT NULL COMMENT '消耗Token数',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`summary_id`),
  KEY `idx_note_summary_notebook` (`notebook_id`),
  KEY `idx_note_summary_source` (`source_id`),
  KEY `idx_note_summary_type` (`summary_type`),
  CONSTRAINT `fk_note_summary_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_note_summary_source` FOREIGN KEY (`source_id`) REFERENCES `notebook_source` (`source_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成摘要表';

-- 66. 思维导图表
CREATE TABLE `mind_map` (
  `map_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '导图ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID(可跨来源)',
  `structure` JSON NOT NULL COMMENT '导图结构(JSON树形: 节点/子节点/标签)',
  `model_used` VARCHAR(64) DEFAULT NULL COMMENT '使用的AI模型',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`map_id`),
  KEY `idx_mind_map_notebook` (`notebook_id`),
  KEY `idx_mind_map_source` (`source_id`),
  CONSTRAINT `fk_mind_map_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_mind_map_source` FOREIGN KEY (`source_id`) REFERENCES `notebook_source` (`source_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='思维导图表(可交互)';

-- 67. 闪卡表
CREATE TABLE `flashcard` (
  `card_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '闪卡ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
  `front` TEXT NOT NULL COMMENT '正面(问题/术语)',
  `back` TEXT NOT NULL COMMENT '背面(答案/解释)',
  `difficulty` TINYINT DEFAULT NULL COMMENT '难度: 1-5(间隔重复用)',
  `next_review_date` DATE DEFAULT NULL COMMENT '下次复习日期(SRS算法)',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '复习次数',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`card_id`),
  KEY `idx_flashcard_notebook` (`notebook_id`),
  KEY `idx_flashcard_source` (`source_id`),
  KEY `idx_flashcard_review_date` (`next_review_date`),
  CONSTRAINT `fk_flashcard_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_flashcard_source` FOREIGN KEY (`source_id`) REFERENCES `notebook_source` (`source_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闪卡表(Anki风格间隔重复)';

-- 68. 音频概述表
CREATE TABLE `audio_overview` (
  `audio_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '音频ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
  `audio_url` VARCHAR(1024) NOT NULL COMMENT '音频文件URL',
  `script` TEXT DEFAULT NULL COMMENT '对话脚本文本',
  `duration` INT DEFAULT NULL COMMENT '音频时长(秒)',
  `model_used` VARCHAR(64) DEFAULT NULL COMMENT 'TTS模型',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`audio_id`),
  KEY `idx_audio_overview_notebook` (`notebook_id`),
  KEY `idx_audio_overview_source` (`source_id`),
  CONSTRAINT `fk_audio_overview_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_audio_overview_source` FOREIGN KEY (`source_id`) REFERENCES `notebook_source` (`source_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音频概述表(Podcast Mode)';

-- 69. 对话记录表
CREATE TABLE `notebook_chat` (
  `chat_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question` TEXT NOT NULL COMMENT '用户提问',
  `answer` TEXT NOT NULL COMMENT 'AI回答',
  `source_refs` JSON DEFAULT NULL COMMENT '引用来源(段落/时间戳/文件列表)',
  `model_used` VARCHAR(64) DEFAULT NULL COMMENT '使用的AI模型',
  `token_count` INT DEFAULT NULL COMMENT '消耗Token数',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '对话会话ID(多轮对话)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`chat_id`),
  KEY `idx_nb_chat_notebook` (`notebook_id`),
  KEY `idx_nb_chat_user` (`user_id`),
  KEY `idx_nb_chat_session` (`session_id`),
  KEY `idx_nb_chat_created` (`notebook_id`, `created_at`),
  CONSTRAINT `fk_nb_chat_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_nb_chat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记录表(引用式回答+溯源)';

-- 70. 笔记编辑表
CREATE TABLE `note_edit` (
  `edit_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '编辑ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `user_id` BIGINT NOT NULL COMMENT '编辑人ID',
  `edit_type` VARCHAR(32) NOT NULL COMMENT '编辑类型: edit-编辑/annotation-批注/highlight-高亮',
  `content_diff` TEXT DEFAULT NULL COMMENT '编辑差异(增量)',
  `position_ref` VARCHAR(255) DEFAULT NULL COMMENT '编辑位置引用(段落ID/时间戳)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`edit_id`),
  KEY `idx_note_edit_notebook` (`notebook_id`),
  KEY `idx_note_edit_user` (`user_id`),
  KEY `idx_note_edit_type` (`edit_type`),
  CONSTRAINT `fk_note_edit_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_note_edit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记编辑表(人工编辑/批注/高亮)';

-- 71. 笔记版本表
CREATE TABLE `note_version` (
  `version_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `version_number` INT NOT NULL COMMENT '版本号(递增)',
  `snapshot` LONGTEXT NOT NULL COMMENT '版本快照(完整笔记内容)',
  `change_summary` VARCHAR(512) DEFAULT NULL COMMENT '变更摘要',
  `created_by` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`version_id`),
  KEY `idx_note_version_notebook` (`notebook_id`),
  KEY `idx_note_version_number` (`notebook_id`, `version_number`),
  CONSTRAINT `fk_note_version_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记版本表(版本回溯)';

-- 72. 笔记分享表
CREATE TABLE `notebook_share` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `share_type` VARCHAR(32) NOT NULL COMMENT '分享类型: public-公开/course_inner-课程内',
  `share_code` VARCHAR(32) DEFAULT NULL COMMENT '分享码(用于fork)',
  `forked_from` BIGINT DEFAULT NULL COMMENT '被fork来源的分享记录ID',
  `fork_count` INT NOT NULL DEFAULT 0 COMMENT '被fork次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notebook_share` (`notebook_id`, `share_type`),
  KEY `idx_nb_share_code` (`share_code`),
  KEY `idx_nb_share_forked_from` (`forked_from`),
  CONSTRAINT `fk_nb_share_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记分享表(fork与协作)';

-- 73. 知识点实体表
CREATE TABLE `knowledge_entity` (
  `entity_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '实体ID',
  `notebook_id` BIGINT NOT NULL COMMENT '笔记本ID',
  `name` VARCHAR(255) NOT NULL COMMENT '知识点名称',
  `description` TEXT DEFAULT NULL COMMENT '知识点描述',
  `entity_type` VARCHAR(32) NOT NULL DEFAULT 'concept' COMMENT '实体类型: concept-概念/person-人物/method-方法/theory-理论/tool-工具',
  `source_refs` JSON DEFAULT NULL COMMENT '来源引用(关联的notebook_source段落)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`entity_id`),
  KEY `idx_knowledge_entity_notebook` (`notebook_id`),
  KEY `idx_knowledge_entity_type` (`entity_type`),
  KEY `idx_knowledge_entity_name` (`name`),
  CONSTRAINT `fk_knowledge_entity_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`notebook_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点实体表(知识图谱节点)';

-- 74. 知识点关联表
CREATE TABLE `knowledge_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_entity_id` BIGINT NOT NULL COMMENT '起始实体ID',
  `to_entity_id` BIGINT NOT NULL COMMENT '目标实体ID',
  `relation_type` VARCHAR(32) NOT NULL COMMENT '关联类型: prerequisite-前置/contains-包含/related-相关/opposite-对立/applied_in-应用于',
  `weight` DECIMAL(3,2) DEFAULT NULL COMMENT '关联强度(0-1)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_relation` (`from_entity_id`, `to_entity_id`, `relation_type`),
  KEY `idx_knowledge_relation_from` (`from_entity_id`),
  KEY `idx_knowledge_relation_to` (`to_entity_id`),
  CONSTRAINT `fk_knowledge_relation_from` FOREIGN KEY (`from_entity_id`) REFERENCES `knowledge_entity` (`entity_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_knowledge_relation_to` FOREIGN KEY (`to_entity_id`) REFERENCES `knowledge_entity` (`entity_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关联表(知识图谱边)';

-- 75. 知识薄弱标记表
CREATE TABLE `knowledge_weakness` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `entity_id` BIGINT NOT NULL COMMENT '知识点实体ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `weak_type` VARCHAR(32) NOT NULL COMMENT '薄弱类型: frequent_question-频繁提问/marked_ununderstood-标记未理解/low_quiz_score-测验低分',
  `occurrence_count` INT NOT NULL DEFAULT 1 COMMENT '发生次数',
  `last_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近发生时间',
  `remind_at` DATETIME DEFAULT NULL COMMENT '下次复习提醒时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_weakness` (`entity_id`, `user_id`, `weak_type`),
  KEY `idx_knowledge_weakness_entity` (`entity_id`),
  KEY `idx_knowledge_weakness_user` (`user_id`),
  KEY `idx_knowledge_weakness_remind` (`user_id`, `remind_at`),
  CONSTRAINT `fk_knowledge_weakness_entity` FOREIGN KEY (`entity_id`) REFERENCES `knowledge_entity` (`entity_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_knowledge_weakness_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识薄弱标记表(薄弱环节可视化)';

-- 76. AI引擎配置表
CREATE TABLE `ai_engine_config` (
  `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `model_name` VARCHAR(64) NOT NULL COMMENT '模型名称(如gpt-4o, gemini-1.5-pro)',
  `model_provider` VARCHAR(32) NOT NULL COMMENT '模型提供商: openai/google/anthropic/local',
  `allowed_domains` JSON DEFAULT NULL COMMENT '允许解析的外部域名白名单(JSON数组)',
  `daily_quota_per_user` INT NOT NULL DEFAULT 100 COMMENT '每用户每日调用配额',
  `max_tokens_per_request` INT NOT NULL DEFAULT 4096 COMMENT '单次请求最大Token数',
  `parameter_overrides` JSON DEFAULT NULL COMMENT '模型参数覆盖(temperature/top_p等)',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  KEY `idx_ai_config_provider` (`model_provider`),
  KEY `idx_ai_config_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI引擎配置表(管理员配置)';

-- ============================================================
-- M11 创新功能模块 (5张表)
-- ============================================================

-- 77. 编程节点表
CREATE TABLE `code_node` (
  `node_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `resource_id` BIGINT NOT NULL COMMENT '视频资源ID',
  `trigger_time` INT NOT NULL COMMENT '触发时间点(秒, 视频播放到此处暂停)',
  `title` VARCHAR(255) NOT NULL COMMENT '节点标题',
  `description` TEXT DEFAULT NULL COMMENT '节点描述/题目说明',
  `template_code` TEXT DEFAULT NULL COMMENT '代码模板(预填充)',
  `test_cases` JSON NOT NULL COMMENT '测试用例(JSON: input/expected_output)',
  `pass_condition` VARCHAR(255) DEFAULT NULL COMMENT '通过条件描述',
  `language` VARCHAR(32) NOT NULL DEFAULT 'python' COMMENT '编程语言: python/java/cpp/javascript',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`node_id`),
  KEY `idx_code_node_resource` (`resource_id`),
  KEY `idx_code_node_trigger` (`resource_id`, `trigger_time`),
  CONSTRAINT `fk_code_node_resource` FOREIGN KEY (`resource_id`) REFERENCES `content_resource` (`resource_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编程节点表(视频中插入编程练习)';

-- 78. 编程提交表
CREATE TABLE `code_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id` BIGINT NOT NULL COMMENT '编程节点ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `code` TEXT NOT NULL COMMENT '提交代码',
  `is_passed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过: 1-是 0-否',
  `feedback` TEXT DEFAULT NULL COMMENT '运行反馈/错误信息',
  `execution_time` INT DEFAULT NULL COMMENT '执行耗时(ms)',
  `memory_usage` INT DEFAULT NULL COMMENT '内存使用(KB)',
  `attempt_count` INT NOT NULL DEFAULT 1 COMMENT '尝试次数',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_code_submit_node` (`node_id`),
  KEY `idx_code_submit_student` (`student_id`),
  KEY `idx_code_submit_node_student` (`node_id`, `student_id`),
  CONSTRAINT `fk_code_submit_node` FOREIGN KEY (`node_id`) REFERENCES `code_node` (`node_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_code_submit_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编程提交表';

-- 79. Wiki补充表
CREATE TABLE `wiki_contribution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `contributor_id` BIGINT NOT NULL COMMENT '贡献者ID',
  `content_type` VARCHAR(32) NOT NULL COMMENT '内容类型: literature-文献/viewpoint-观点/code-代码',
  `title` VARCHAR(255) NOT NULL COMMENT '补充标题',
  `content` TEXT NOT NULL COMMENT '补充内容',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源链接',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待审核/adopted-已采纳/rejected-驳回',
  `reviewed_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_wiki_course` (`course_id`),
  KEY `idx_wiki_contributor` (`contributor_id`),
  KEY `idx_wiki_status` (`status`),
  CONSTRAINT `fk_wiki_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_wiki_contributor` FOREIGN KEY (`contributor_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_wiki_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wiki补充表(知识库迭代)';

-- 80. 悬赏任务表
CREATE TABLE `bounty_task` (
  `bounty_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '悬赏ID',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID(教师/企业)',
  `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
  `description` TEXT NOT NULL COMMENT '任务描述',
  `reward_type` VARCHAR(32) NOT NULL COMMENT '奖励类型: points-积分/certification-专属认证',
  `reward_amount` INT DEFAULT NULL COMMENT '奖励数量(积分值等)',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '难度: 1-简单 2-中等 3-困难',
  `status` VARCHAR(32) NOT NULL DEFAULT 'open' COMMENT '状态: open-开放/assigned-已接单/resolved-已解决/closed-已关闭',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '接单人ID',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`bounty_id`),
  KEY `idx_bounty_publisher` (`publisher_id`),
  KEY `idx_bounty_status` (`status`),
  KEY `idx_bounty_assignee` (`assignee_id`),
  CONSTRAINT `fk_bounty_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_bounty_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='悬赏任务表(难点悬赏)';

-- 81. 悬赏提交表
CREATE TABLE `bounty_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bounty_id` BIGINT NOT NULL COMMENT '悬赏ID',
  `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
  `solution` TEXT NOT NULL COMMENT '解决方案',
  `attachment_urls` JSON DEFAULT NULL COMMENT '附件URL列表',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待审核/adopted-已采纳/rejected-未采纳',
  `reviewed_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号', -- [v2.0 FIX]
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bounty_submit_bounty` (`bounty_id`),
  KEY `idx_bounty_submit_submitter` (`submitter_id`),
  KEY `idx_bounty_submit_status` (`status`),
  CONSTRAINT `fk_bounty_submit_bounty` FOREIGN KEY (`bounty_id`) REFERENCES `bounty_task` (`bounty_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_bounty_submit_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,  -- [v2.0 FIX]

  CONSTRAINT `fk_bounty_submit_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='悬赏提交表';

-- ============================================================
-- M12 系统管理模块 (5张表) -- [v2.0 FIX: +operation_log]
-- ============================================================

-- 82. 平台模块管理表
CREATE TABLE `platform_module` (
  `module_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模块ID',
  `module_name` VARCHAR(128) NOT NULL COMMENT '模块名称',
  `module_code` VARCHAR(64) NOT NULL COMMENT '模块编码(如M1_USER_AUTH)',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '模块描述',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-是 0-否',
  `config` JSON DEFAULT NULL COMMENT '模块配置(JSON)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`module_id`),
  UNIQUE KEY `uk_platform_module_code` (`module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台模块管理表(模块开关)';

-- 83. 数据大屏指标表
CREATE TABLE `dashboard_metric` (
  `metric_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `metric_type` VARCHAR(32) NOT NULL COMMENT '指标类型: dau-日活/page_view-访问量/completion_rate-完成率/submission_rate-提交率',
  `metric_value` DECIMAL(12,2) NOT NULL COMMENT '指标值',
  `date` DATE NOT NULL COMMENT '统计日期',
  `course_id` BIGINT DEFAULT NULL COMMENT '课程ID(可选, 为空表示全局)',
  `detail` JSON DEFAULT NULL COMMENT '明细数据(JSON)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`metric_id`),
  UNIQUE KEY `uk_dashboard_metric` (`metric_type`, `date`, `course_id`),
  KEY `idx_dashboard_date` (`date`),
  KEY `idx_dashboard_course` (`course_id`),
  CONSTRAINT `fk_dashboard_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE RESTRICT  -- [v2.0 FIX]
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据大屏指标表';

-- 84. 批量导入记录表
CREATE TABLE `batch_import` (
  `import_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '导入ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `import_type` VARCHAR(32) NOT NULL COMMENT '导入类型: teacher-教师/student-学生/course-课程/question-题目/class-行政班级', -- [v2.0 FIX]
  `file_url` VARCHAR(1024) NOT NULL COMMENT '导入文件URL',
  `file_format` VARCHAR(32) NOT NULL COMMENT '文件格式: excel/word/csv',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总记录数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `error_detail` JSON DEFAULT NULL COMMENT '错误详情(行号+原因)',
  `status` VARCHAR(32) NOT NULL DEFAULT 'processing' COMMENT '状态: processing-处理中/completed-完成/failed-失败',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`import_id`),
  KEY `idx_batch_import_operator` (`operator_id`),
  KEY `idx_batch_import_type` (`import_type`),
  KEY `idx_batch_import_status` (`status`),
  CONSTRAINT `fk_batch_import_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量导入记录表';

-- 85. 系统配置表
CREATE TABLE `system_config` (
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `value_type` VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '值类型: string/number/json/boolean',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '配置说明',
  `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开给前端: 1-是 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表(键值对)';

-- [v2.0 FIX] 全局操作审计日志表
CREATE TABLE `operation_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID(系统操作时为NULL)',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名(冗余, 防用户删除后丢失)',
  `operator_ip` VARCHAR(64) DEFAULT NULL COMMENT '操作IP地址',
  `op_type` VARCHAR(32) NOT NULL COMMENT '操作类型: login-登录/create-创建/update-更新/delete-删除/reset_pwd-重置密码/ban-封禁/import-批量导入/export-导出/grade_modify-成绩修改',
  `op_module` VARCHAR(32) NOT NULL COMMENT '操作模块: user/course/task/grade/community/cet/system等',
  `target_type` VARCHAR(64) DEFAULT NULL COMMENT '操作对象类型(如表名)',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作对象ID',
  `op_detail` TEXT DEFAULT NULL COMMENT '操作详情(变更前后对比/关键参数)',
  `request_params` JSON DEFAULT NULL COMMENT '请求参数(脱敏后)',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
  `op_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_op_log_operator` (`operator_id`),
  KEY `idx_op_log_type` (`op_type`),
  KEY `idx_op_log_module` (`op_module`),
  KEY `idx_op_log_time` (`op_time`),
  KEY `idx_op_log_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局操作审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
