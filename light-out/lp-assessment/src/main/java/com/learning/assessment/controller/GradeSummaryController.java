package com.learning.assessment.controller;

import com.learning.assessment.service.GradeSummaryService;
import com.learning.assessment.vo.GradeSummaryVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gradeSummary")
public class GradeSummaryController {

    @Resource
    private GradeSummaryService gradeSummaryService;

    // ========== 成绩汇总 ==========

    @PostMapping("/summarize/{courseId}")
    public Result<List<GradeSummaryVO>> summarize(@PathVariable Long courseId) {
        return Result.success(gradeSummaryService.summarize(courseId));
    }

    @GetMapping("/list/{courseId}")
    public Result<PageResult<GradeSummaryVO>> listByCourse(@PathVariable Long courseId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gradeSummaryService.listByCourse(courseId, page, pageSize));
    }

    // ========== 教师确认 ==========

    @PostMapping("/confirm/{courseId}")
    public Result<List<GradeSummaryVO>> confirm(@PathVariable Long courseId) {
        return Result.success(gradeSummaryService.confirm(courseId));
    }

    // ========== 导出成绩单 ==========

    @GetMapping("/export/{courseId}")
    public void export(@PathVariable Long courseId, HttpServletResponse response) {
        gradeSummaryService.exportExcel(courseId, response);
    }
}
