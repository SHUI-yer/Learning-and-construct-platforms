package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.CourseCopy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseCopyMapper {
    void insert(CourseCopy copyRecord);
}