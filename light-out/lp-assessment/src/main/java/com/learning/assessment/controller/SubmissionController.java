package com.learning.assessment.controller;

import com.learning.assessment.dto.SubmissionGradeDTO;
import com.learning.assessment.dto.SubmissionExcellentDTO;
import com.learning.assessment.dto.SubmissionSubmitDTO;
import com.learning.assessment.service.SubmissionService;
import com.learning.assessment.vo.ExamStartVO;
import com.learning.assessment.vo.SubmissionVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submission")
public class SubmissionController {

    @Resource
    private SubmissionService submissionService;

    // ========== 限时作答（开始计时） ==========

    @PostMapping("/start")
    public Result<ExamStartVO> start(@RequestParam Long taskId) {
        return Result.success(submissionService.startExam(taskId));
    }

    // ========== 学生作答（quiz 自动判分） ==========

    @PostMapping("/submit")
    public Result<SubmissionVO> submit(@RequestBody SubmissionSubmitDTO dto) {
        SubmissionVO result = submissionService.submitQuiz(dto);
        return Result.success(result);
    }

    // ========== 教师批阅 ==========

    @GetMapping("/list/{taskId}")
    public Result<PageResult<SubmissionVO>> listByTask(@PathVariable Long taskId,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String status) {
        PageResult<SubmissionVO> result = submissionService.listByTask(taskId, page, pageSize, status);
        return Result.success(result);
    }

    @GetMapping("/{submissionId}")
    public Result<SubmissionVO> detail(@PathVariable Long submissionId) {
        SubmissionVO result = submissionService.detail(submissionId);
        return Result.success(result);
    }

    @PostMapping("/grade")
    public Result<SubmissionVO> grade(@RequestBody SubmissionGradeDTO dto) {
        SubmissionVO result = submissionService.grade(dto);
        return Result.success(result);
    }

    // ========== 优秀范本 ==========

    @PostMapping("/excellent")
    public Result<SubmissionVO> setExcellent(@RequestBody SubmissionExcellentDTO dto) {
        SubmissionVO result = submissionService.setExcellent(dto);
        return Result.success(result);
    }

    @GetMapping("/excellent/list/{taskId}")
    public Result<PageResult<SubmissionVO>> listExcellent(@PathVariable Long taskId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<SubmissionVO> result = submissionService.listExcellent(taskId, page, pageSize);
        return Result.success(result);
    }
}
