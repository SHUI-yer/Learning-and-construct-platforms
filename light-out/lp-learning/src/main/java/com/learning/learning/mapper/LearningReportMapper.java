package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.LearningReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LearningReportMapper extends BaseMapper<LearningReport> {

    LearningReport selectByUserCourseTypePeriod(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("reportType") String reportType, @Param("period") String period);
}
