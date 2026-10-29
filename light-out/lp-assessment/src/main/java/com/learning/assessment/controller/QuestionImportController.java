package com.learning.assessment.controller;

import com.learning.assessment.service.QuestionImportService;
import com.learning.assessment.vo.QuestionImportVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/question/import")
public class QuestionImportController {

    @Resource
    private QuestionImportService questionImportService;

    @PostMapping("/upload")
    public Result<QuestionImportVO> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam Long courseId) {
        return Result.success(questionImportService.importQuestions(file, courseId));
    }

    @GetMapping("/list")
    public Result<PageResult> list(@RequestParam Long courseId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(questionImportService.listImports(courseId, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<QuestionImportVO> detail(@PathVariable Long id) {
        return Result.success(questionImportService.getImportDetail(id));
    }
}
