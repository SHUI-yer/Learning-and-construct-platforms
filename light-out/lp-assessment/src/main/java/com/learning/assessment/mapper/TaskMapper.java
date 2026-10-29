package com.learning.assessment.mapper;

import com.learning.assessment.entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskMapper {

    @Insert("INSERT INTO task (course_id, chapter_id, title, description, task_type, tag, weight, " +
            "publish_type, publish_time, deadline, allow_late, late_penalty, max_score, allow_resubmit, " +
            "attachment_urls, version, created_at, updated_at) " +
            "VALUES (#{courseId}, #{chapterId}, #{title}, #{description}, #{taskType}, #{tag}, #{weight}, " +
            "#{publishType}, #{publishTime}, #{deadline}, #{allowLate}, #{latePenalty}, #{maxScore}, " +
            "#{allowResubmit}, #{attachmentUrls}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "taskId")
    int insert(Task task);

    @Update("UPDATE task SET deleted_at = NOW(), updated_at = NOW() WHERE task_id = #{taskId}")
    int softDelete(Long taskId);

    @Update("UPDATE task SET title = #{title}, description = #{description}, task_type = #{taskType}, " +
            "tag = #{tag}, weight = #{weight}, publish_type = #{publishType}, publish_time = #{publishTime}, " +
            "deadline = #{deadline}, allow_late = #{allowLate}, late_penalty = #{latePenalty}, " +
            "max_score = #{maxScore}, allow_resubmit = #{allowResubmit}, attachment_urls = #{attachmentUrls}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE task_id = #{taskId} AND version = #{version}")
    int update(Task task);

    @Select("SELECT * FROM task WHERE task_id = #{taskId} AND deleted_at IS NULL")
    Task selectById(Long taskId);

    @SelectProvider(type = TaskSqlProvider.class, method = "selectByConditions")
    List<Task> selectByConditions(@Param("courseId") Long courseId,
                                   @Param("taskType") String taskType,
                                   @Param("keyword") String keyword);

    @Select("SELECT * FROM task WHERE course_id = #{courseId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<Task> selectByCourseId(Long courseId);

    class TaskSqlProvider {
        public String selectByConditions(@Param("courseId") Long courseId,
                                         @Param("taskType") String taskType,
                                         @Param("keyword") String keyword) {
            StringBuilder sql = new StringBuilder("SELECT * FROM task WHERE deleted_at IS NULL");
            if (courseId != null) {
                sql.append(" AND course_id = #{courseId}");
            }
            if (taskType != null && !taskType.isEmpty()) {
                sql.append(" AND task_type = #{taskType}");
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))");
            }
            sql.append(" ORDER BY created_at DESC");
            return sql.toString();
        }
    }
}