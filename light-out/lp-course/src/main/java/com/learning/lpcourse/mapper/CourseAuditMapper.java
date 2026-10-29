package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.CourseAudit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseAuditMapper {
    void insert(CourseAudit audit);
}