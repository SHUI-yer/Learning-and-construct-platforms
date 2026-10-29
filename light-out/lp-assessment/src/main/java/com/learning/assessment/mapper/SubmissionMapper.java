package com.learning.assessment.mapper;

import com.learning.assessment.entity.Submission;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SubmissionMapper {

    @Insert("INSERT INTO submission (task_id, student_id, submit_type, content, attachment_urls, " +
            "submitted_at, is_late, is_excellent, status, teacher_comment, score, version, created_at, updated_at) " +
            "VALUES (#{taskId}, #{studentId}, #{submitType}, #{content}, #{attachmentUrls}, " +
            "#{submittedAt}, #{isLate}, #{isExcellent}, #{status}, #{teacherComment}, #{score}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "submissionId")
    int insert(Submission submission);

    @Update("UPDATE submission SET status = #{status}, submitted_at = #{submittedAt}, " +
            "teacher_comment = #{teacherComment}, score = #{score}, is_excellent = #{isExcellent}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE submission_id = #{submissionId} AND version = #{version}")
    int update(Submission submission);

    @Update("UPDATE submission SET status = 'in_progress', submitted_at = #{submittedAt}, " +
            "score = NULL, teacher_comment = NULL, is_excellent = 0, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE submission_id = #{submissionId} AND student_id = #{studentId}")
    int resetToInProgress(@Param("submissionId") Long submissionId,
                          @Param("studentId") Long studentId,
                          @Param("submittedAt") LocalDateTime submittedAt);

    @Select("SELECT * FROM submission WHERE submission_id = #{submissionId}")
    Submission selectById(Long submissionId);

    @Select("SELECT * FROM submission WHERE task_id = #{taskId} AND student_id = #{studentId}")
    Submission selectByTaskAndStudent(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM submission WHERE task_id = #{taskId} ORDER BY submitted_at DESC")
    List<Submission> selectByTaskId(Long taskId);

    @Select("SELECT * FROM submission WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<Submission> selectByStudentId(Long studentId);

    @Select("SELECT * FROM submission WHERE task_id = #{taskId} AND status = #{status} ORDER BY submitted_at ASC")
    List<Submission> selectByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") String status);

    @Select("SELECT * FROM submission WHERE task_id = #{taskId} AND is_excellent = 1 " +
            "ORDER BY submitted_at ASC LIMIT #{offset}, #{pageSize}")
    List<Submission> selectExcellentPageByTaskId(@Param("taskId") Long taskId,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM submission WHERE task_id = #{taskId} AND is_excellent = 1")
    long countExcellentByTaskId(Long taskId);

    @Select("SELECT COUNT(*) FROM submission WHERE task_id = #{taskId} AND status = 'pending'")
    int countPendingByTaskId(Long taskId);

    /**
     * 按任务分页查询提交。
     *
     * <p>{@code studentId} 非空时只返回该学生的提交：学生端只有权看自己的记录，
     * 由服务层按角色传入该条件，避免把全班提交下发到学生浏览器。</p>
     */
    @SelectProvider(type = SubmissionSqlProvider.class, method = "selectPageByTaskId")
    List<Submission> selectPageByTaskId(@Param("taskId") Long taskId,
                                        @Param("status") String status,
                                        @Param("studentId") Long studentId,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    @SelectProvider(type = SubmissionSqlProvider.class, method = "countByTaskId")
    long countByTaskId(@Param("taskId") Long taskId,
                       @Param("status") String status,
                       @Param("studentId") Long studentId);

    class SubmissionSqlProvider {
        /** 仅拼接常量片段，参数全部走 #{}, 不存在 SQL 注入面 */
        public String buildWhere(Long taskId, String status, Long studentId) {
            StringBuilder sql = new StringBuilder(" WHERE task_id = #{taskId}");
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = #{status}");
            }
            if (studentId != null) {
                sql.append(" AND student_id = #{studentId}");
            }
            return sql.toString();
        }

        public String selectPageByTaskId(Long taskId, String status, Long studentId, int offset, int pageSize) {
            return "SELECT * FROM submission" + buildWhere(taskId, status, studentId)
                    + " ORDER BY submitted_at DESC LIMIT #{offset}, #{pageSize}";
        }

        public String countByTaskId(Long taskId, String status, Long studentId) {
            return "SELECT COUNT(*) FROM submission" + buildWhere(taskId, status, studentId);
        }
    }
}
