package com.learning.learning.controller;

import com.learning.learning.dto.ReportQueryDTO;
import com.learning.learning.service.LearningReportService;
import com.learning.learning.vo.LearningReportVO;
import com.learning.lpcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学习报告")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/report")
public class LearningReportController {

    private final LearningReportService learningReportService;

    @Operation(summary = "查看学习报告")
    @GetMapping
    public Result<LearningReportVO> getReport(@RequestParam Long userId,
                                              @RequestParam Long courseId,
                                              @RequestParam(required = false, defaultValue = "weekly") String type,
                                              @RequestParam(required = false, defaultValue = "week") String period) {
        // 先查已有报告，不存在则自动生成
        LearningReportVO report = learningReportService.getOrGenerateReport(userId, courseId, type, period);
        // 补充成功文案，解决msg=null
        return Result.success(report, "查询学习报告成功");
    }

    @Operation(summary = "生成学习报告")
    @PostMapping("/generate")
    public Result<LearningReportVO> generate(@RequestBody @Valid ReportQueryDTO dto) {
        // userId/courseId 统一从请求体 DTO 获取（已通过 @NotNull 校验非空），不再混用 @RequestParam
        // reportType/period 为空时回退默认值，保证返回 VO 子字段非空
        String reportType = dto.getReportType() == null || dto.getReportType().isBlank() ? "weekly" : dto.getReportType();
        String period = dto.getPeriod() == null || dto.getPeriod().isBlank() ? "week" : dto.getPeriod();
        LearningReportVO vo = learningReportService.generateReport(
                dto.getUserId(),
                dto.getCourseId(),
                reportType,
                period
        );
        // 传入固定成功文案，保证顶层 msg 不为 null
        return Result.success(vo, "学习报告生成任务已提交，请稍后查看报告详情");
    }
}