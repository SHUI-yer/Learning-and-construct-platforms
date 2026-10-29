package com.learning.assessment.mapper;

import com.learning.assessment.entity.GradeAppeal;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GradeAppealMapper {

    @Insert("INSERT INTO grade_appeal (grade_id, student_id, appeal_reason, teacher_response, " +
            "new_score, status, created_at, resolved_at, version, updated_at) " +
            "VALUES (#{gradeId}, #{studentId}, #{appealReason}, #{teacherResponse}, " +
            "#{newScore}, #{status}, NOW(), #{resolvedAt}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "appealId")
    int insert(GradeAppeal gradeAppeal);

    @Update("UPDATE grade_appeal SET teacher_response = #{teacherResponse}, " +
            "new_score = #{newScore}, status = #{status}, resolved_at = #{resolvedAt}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE appeal_id = #{appealId} AND version = #{version}")
    int update(GradeAppeal gradeAppeal);

    @Select("SELECT * FROM grade_appeal WHERE appeal_id = #{appealId}")
    GradeAppeal selectById(Long appealId);

    @Select("SELECT * FROM grade_appeal WHERE grade_id = #{gradeId}")
    List<GradeAppeal> selectByGradeId(Long gradeId);

    @Select("SELECT * FROM grade_appeal WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<GradeAppeal> selectByStudentId(Long studentId);

    @Select("SELECT * FROM grade_appeal WHERE status = #{status} ORDER BY created_at ASC")
    List<GradeAppeal> selectByStatus(String status);
    @Select("<script>" +
            "SELECT ga.* FROM grade_appeal ga JOIN grade g ON ga.grade_id = g.grade_id " +
            "WHERE g.course_id = #{courseId} " +
            "<if test='status != null and status != \"\"'>AND ga.status = #{status}</if> " +
            "ORDER BY ga.created_at DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<GradeAppeal> selectPageByCourseId(@Param("courseId") Long courseId,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM grade_appeal ga JOIN grade g ON ga.grade_id = g.grade_id " +
            "WHERE g.course_id = #{courseId} " +
            "<if test='status != null and status != \"\"'>AND ga.status = #{status}</if>" +
            "</script>")
    long countByCourseId(@Param("courseId") Long courseId, @Param("status") String status);

    @Select("SELECT * FROM grade_appeal WHERE student_id = #{studentId} ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<GradeAppeal> selectPageByStudentId(@Param("studentId") Long studentId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM grade_appeal WHERE student_id = #{studentId}")
    long countByStudentId(Long studentId);
}
