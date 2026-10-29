package com.learning.assessment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程归属的只读查询。
 *
 * <p>考核模块需要判断"当前教师是否属于这门课"，但课程数据在 lp-course 模块。
 * 这里只做只读查询，不改课程数据，避免为一次校验把两个业务模块强耦合起来。</p>
 */
@Mapper
public interface CourseAccessMapper {

    /** 课程创建者（course.teacher_id）；课程不存在或已软删返回 null */
    @Select("SELECT teacher_id FROM course WHERE course_id = #{courseId} AND deleted_at IS NULL")
    Long selectCourseOwner(Long courseId);

    /** 是否为该课程的授课教师/助教（course_teacher 关联表） */
    @Select("SELECT COUNT(*) FROM course_teacher WHERE course_id = #{courseId} AND teacher_id = #{teacherId}")
    int countCourseTeacher(@Param("courseId") Long courseId, @Param("teacherId") Long teacherId);
}
