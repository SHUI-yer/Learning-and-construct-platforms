package com.learning.assessment.mapper;

import com.learning.assessment.entity.Grade;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GradeMapper {

    @Insert("INSERT INTO grade (course_id, student_id, task_id, submission_id, score, weighted_score, " +
            "component_type, is_confirmed, version, created_at, updated_at) " +
            "VALUES (#{courseId}, #{studentId}, #{taskId}, #{submissionId}, #{score}, #{weightedScore}, " +
            "#{componentType}, #{isConfirmed}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "gradeId")
    int insert(Grade grade);

    @Update("UPDATE grade SET score = #{score}, weighted_score = #{weightedScore}, " +
            "is_confirmed = #{isConfirmed}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE grade_id = #{gradeId} AND version = #{version}")
    int update(Grade grade);

    @Select("SELECT * FROM grade WHERE grade_id = #{gradeId}")
    Grade selectById(Long gradeId);

    @Select("SELECT * FROM grade WHERE course_id = #{courseId} AND student_id = #{studentId} ORDER BY task_id")
    List<Grade> selectByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM grade WHERE task_id = #{taskId} AND student_id = #{studentId}")
    Grade selectByTaskAndStudent(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM grade WHERE submission_id = #{submissionId}")
    Grade selectBySubmissionId(Long submissionId);

    @Select("SELECT * FROM grade WHERE task_id = #{taskId} ORDER BY score DESC")
    List<Grade> selectByTaskId(Long taskId);

    @Select("SELECT COALESCE(SUM(weighted_score), 0) FROM grade WHERE course_id = #{courseId} AND student_id = #{studentId} AND is_confirmed = 1")
    java.math.BigDecimal sumWeightedScoreByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
    @Select("SELECT DISTINCT student_id FROM grade WHERE course_id = #{courseId}")
    List<Long> selectStudentIdsByCourseId(Long courseId);
}
