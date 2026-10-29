package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.StudyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StudyStatsMapper extends BaseMapper<StudyStats> {
    StudyStats selectByUserCourseDate(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("statDate") LocalDate statDate);

    int upsert(StudyStats entity);

    int incrementDuration(@Param("userId") Long userId,
                          @Param("courseId") Long courseId,
                          @Param("statDate") LocalDate statDate,
                          @Param("videoDelta") int videoDelta,
                          @Param("docDelta") int docDelta,
                          @Param("totalDelta") int totalDelta);

    Long countDistinctUsersByDate(@Param("date") String date);

    Long countDistinctUsersSince(@Param("startDate") String startDate);

    java.math.BigDecimal avgCompletionForCourse(@Param("courseId") Long courseId);

    java.util.List<java.util.Map<String, Object>> courseVisitRanking(@Param("startDate") String startDate, @Param("limit") int limit);

    java.math.BigDecimal assignmentSubmissionRate(@Param("courseId") Long courseId);
}
