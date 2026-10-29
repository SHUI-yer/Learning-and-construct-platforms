package com.learning.assessment.mapper;

import com.learning.assessment.entity.QuestionImport;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QuestionImportMapper {

    @Insert("INSERT INTO question_import (course_id, operator_id, file_url, file_format, import_count, " +
            "success_count, fail_count, error_detail, status, created_at, updated_at) " +
            "VALUES (#{courseId}, #{operatorId}, #{fileUrl}, #{fileFormat}, #{importCount}, " +
            "#{successCount}, #{failCount}, #{errorDetail}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionImport questionImport);

    @Update("UPDATE question_import SET import_count = #{importCount}, success_count = #{successCount}, " +
            "fail_count = #{failCount}, error_detail = #{errorDetail}, status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(QuestionImport questionImport);

    @Select("SELECT * FROM question_import WHERE id = #{id}")
    QuestionImport selectById(Long id);

    @Select("SELECT * FROM question_import WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<QuestionImport> selectByCourseId(Long courseId);

    @Select("SELECT COUNT(*) FROM question_import WHERE course_id = #{courseId}")
    long countByCourseId(Long courseId);

    @Select("SELECT * FROM question_import WHERE course_id = #{courseId} ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<QuestionImport> selectByCourseIdPage(@Param("courseId") Long courseId,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);
}
