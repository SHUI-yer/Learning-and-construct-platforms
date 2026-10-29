package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.CourseStudent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CourseStudentMapper {
    void insertBatch(@Param("list") List<CourseStudent> list);
    // 查询课程已存在学生ID（去重防止唯一索引冲突）
    List<Long> selectStudentIdByCourseId(@Param("courseId") Long courseId);
}