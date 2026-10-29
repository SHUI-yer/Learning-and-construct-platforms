package com.learning.learning.service;

import com.learning.learning.vo.LearningReportVO;

public interface LearningReportService {

    LearningReportVO generateReport(Long userId, Long courseId, String type, String period);

    LearningReportVO getReport(Long reportId);

    LearningReportVO getOrGenerateReport(Long userId, Long courseId, String type, String period);
}
