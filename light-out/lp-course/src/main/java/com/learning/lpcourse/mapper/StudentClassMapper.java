package com.learning.lpcourse.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentClassMapper {
    /**
     * 根据班级id集合 查询所有学生id
     */
    List<Long> selectStudentIdByClassIds(@Param("classIdList") List<Long> classIdList);
}