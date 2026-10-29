package com.learning.learning.controller;

import com.learning.learning.service.StudyStatsService;
import com.learning.learning.vo.CourseProgressVO;
import com.learning.learning.vo.DashboardVO;
import com.learning.lpcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "学习统计")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/stats")
public class StudyStatsController {

    private final StudyStatsService studyStatsService;

    @Operation(summary = "查询我的课程进度")
    @GetMapping("/progress")
    public Result<CourseProgressVO> getProgress(@RequestParam Long userId,
                                                @RequestParam Long courseId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CourseProgressVO progressVO = studyStatsService.getStats(userId, courseId, date == null ? LocalDate.now() : date);
        // 补充自定义成功提示文案，解决msg为null校验报错
        return Result.success(progressVO, "查询课程学习进度统计成功");
    }

    @Operation(summary = "查询数据大屏")
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(@RequestParam(required = false) Long courseId) {
        DashboardVO dashboardVO = studyStatsService.getDashboard(courseId);
        // 补充自定义成功提示文案，避免后续msg为空校验问题
        return Result.success(dashboardVO, "查询学习数据大屏统计成功");
    }
}
