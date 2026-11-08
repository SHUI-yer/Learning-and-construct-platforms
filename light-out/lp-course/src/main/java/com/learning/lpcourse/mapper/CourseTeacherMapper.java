package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.CourseTeacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseTeacherMapper {
    int insert(CourseTeacher courseTeacher);
    int countByCourseAndTeacher(@Param("courseId") Long courseId,
                                @Param("teacherId") Long teacherId);
}
