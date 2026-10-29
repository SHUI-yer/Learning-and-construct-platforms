package com.learning.learning.controller;

import com.learning.lpcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.learning.learning.service.LearningMonitorService;

@Tag(name = "学习监控")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/monitor")
public class LearningMonitorController {

    private final LearningMonitorService learningMonitorService;

    @Operation(summary = "全班进度监控")
    @GetMapping("/progress")
    public Result<java.util.Map<String, Object>> getMonitorProgress(@RequestParam(required = false) Long courseId,
                                                                    @RequestParam(required = false) Long chapterId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        var data = learningMonitorService.getClassProgress(courseId, chapterId, page, size);
        // 传入固定成功提示文案，不再默认msg=null
        return Result.success(
                java.util.Map.of(
                        "courseId", courseId,
                        "chapterId", chapterId,
                        "page", page,
                        "size", size,
                        "items", data
                ),
                "查询全班学习进度监控数据成功"
        );
    }
    }
