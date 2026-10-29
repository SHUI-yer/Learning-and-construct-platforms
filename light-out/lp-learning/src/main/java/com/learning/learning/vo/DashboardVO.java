package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {

    private Long dau;
    private Long weeklyActive;
    private BigDecimal avgCompletion;
    private BigDecimal assignmentSubmissionRate;
    private List<String> topChapters;
    private List<CourseRankingVO> courseRanking;
}
