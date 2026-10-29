package com.learning.assessment.mapper;

import com.learning.assessment.entity.GradeScheme;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GradeSchemeMapper {

    @Insert("INSERT INTO grade_scheme (course_id, component_type, weight, sort_order, created_at, updated_at) " +
            "VALUES (#{courseId}, #{componentType}, #{weight}, #{sortOrder}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "schemeId")
    int insert(GradeScheme gradeScheme);

    @Delete("DELETE FROM grade_scheme WHERE scheme_id = #{schemeId}")
    int deleteById(Long schemeId);

    @Update("UPDATE grade_scheme SET weight = #{weight}, sort_order = #{sortOrder}, updated_at = NOW() " +
            "WHERE scheme_id = #{schemeId}")
    int update(GradeScheme gradeScheme);

    @Select("SELECT * FROM grade_scheme WHERE scheme_id = #{schemeId}")
    GradeScheme selectById(Long schemeId);

    @Select("SELECT * FROM grade_scheme WHERE course_id = #{courseId} ORDER BY sort_order ASC")
    List<GradeScheme> selectByCourseId(Long courseId);

    @Select("SELECT COALESCE(SUM(weight), 0) FROM grade_scheme WHERE course_id = #{courseId}")
    java.math.BigDecimal sumWeightByCourseId(Long courseId);

    @Select("SELECT * FROM grade_scheme WHERE course_id = #{courseId} AND component_type = #{componentType}")
    GradeScheme selectByCourseIdAndType(@Param("courseId") Long courseId,
                                        @Param("componentType") String componentType);

    @Delete("DELETE FROM grade_scheme WHERE course_id = #{courseId}")
    int deleteByCourseId(Long courseId);
}