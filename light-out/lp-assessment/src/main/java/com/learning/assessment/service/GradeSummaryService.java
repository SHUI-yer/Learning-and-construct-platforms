package com.learning.assessment.service;

import com.learning.assessment.vo.GradeSummaryVO;
import com.learning.lpcommon.result.PageResult;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface GradeSummaryService {

    List<GradeSummaryVO> summarize(Long courseId);

    PageResult<GradeSummaryVO> listByCourse(Long courseId, Integer page, Integer pageSize);

    List<GradeSummaryVO> confirm(Long courseId);

    void exportExcel(Long courseId, HttpServletResponse response);
}
