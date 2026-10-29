package com.learning.learning.service;

import com.learning.learning.vo.CourseProgressVO;
import com.learning.learning.vo.DashboardVO;

import java.time.LocalDate;

public interface StudyStatsService {

    void addDuration(Long userId, Long courseId, int seconds, String type);

    CourseProgressVO getStats(Long userId, Long courseId, LocalDate date);

    void dailyAggregation();

    DashboardVO getDashboard(Long courseId);
}
