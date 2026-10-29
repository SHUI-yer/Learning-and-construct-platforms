package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.CourseTeacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseTeacherMapper {
    int insert(CourseTeacher courseTeacher);
}