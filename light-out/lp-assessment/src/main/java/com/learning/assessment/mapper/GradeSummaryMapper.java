package com.learning.assessment.mapper;

import com.learning.assessment.entity.GradeSummary;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GradeSummaryMapper {

    @Insert("INSERT INTO grade_summary (course_id, student_id, final_score, rank_in_course, " +
            "confirmed_by, confirmed_at, is_exported, version, created_at, updated_at) " +
            "VALUES (#{courseId}, #{studentId}, #{finalScore}, #{rankInCourse}, " +
            "#{confirmedBy}, #{confirmedAt}, #{isExported}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GradeSummary gradeSummary);

    @Update("UPDATE grade_summary SET final_score = #{finalScore}, rank_in_course = #{rankInCourse}, " +
            "confirmed_by = #{confirmedBy}, confirmed_at = #{confirmedAt}, is_exported = #{isExported}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE id = #{id} AND version = #{version}")
    int update(GradeSummary gradeSummary);

    @Select("SELECT * FROM grade_summary WHERE id = #{id}")
    GradeSummary selectById(Long id);

    @Select("SELECT * FROM grade_summary WHERE course_id = #{courseId} AND student_id = #{studentId}")
    GradeSummary selectByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM grade_summary WHERE course_id = #{courseId} ORDER BY final_score DESC")
    List<GradeSummary> selectByCourseId(Long courseId);

    @Select("SELECT * FROM grade_summary WHERE course_id = #{courseId} ORDER BY rank_in_course ASC")
    List<GradeSummary> selectRankedByCourseId(Long courseId);
    @Select("SELECT * FROM grade_summary WHERE course_id = #{courseId} ORDER BY final_score DESC LIMIT #{offset}, #{pageSize}")
    List<GradeSummary> selectPageByCourseId(@Param("courseId") Long courseId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    @Select("SELECT * FROM grade_summary WHERE course_id = #{courseId} ORDER BY rank_in_course ASC, final_score DESC LIMIT #{offset}, #{pageSize}")
    List<GradeSummary> selectPageRankedByCourseId(@Param("courseId") Long courseId,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM grade_summary WHERE course_id = #{courseId}")
    long countByCourseId(Long courseId);

    @Select("SELECT COUNT(*) FROM grade_summary WHERE course_id = #{courseId} AND confirmed_at IS NOT NULL")
    long countConfirmedByCourseId(Long courseId);
}
