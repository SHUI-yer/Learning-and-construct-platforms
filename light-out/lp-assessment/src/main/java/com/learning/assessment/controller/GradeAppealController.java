package com.learning.assessment.controller;

import com.learning.assessment.dto.GradeAppealCreateDTO;
import com.learning.assessment.dto.GradeAppealReviewDTO;
import com.learning.assessment.service.GradeAppealService;
import com.learning.assessment.vo.GradeAppealVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gradeAppeal")
public class GradeAppealController {

    @Resource
    private GradeAppealService gradeAppealService;

    // ========== 学生申诉 ==========

    @PostMapping("/create")
    public Result<GradeAppealVO> create(@RequestBody GradeAppealCreateDTO dto) {
        return Result.success(gradeAppealService.createAppeal(dto));
    }

    @GetMapping("/my")
    public Result<PageResult<GradeAppealVO>> my(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gradeAppealService.listMyAppeals(page, pageSize));
    }

    // 学生发起申诉前按提交ID反查成绩记录ID，无成绩记录返回 null
    @GetMapping("/grade/{submissionId}")
    public Result<Long> getGradeIdBySubmission(@PathVariable Long submissionId) {
        return Result.success(gradeAppealService.getGradeIdBySubmission(submissionId));
    }

    // ========== 教师复核 ==========

    @GetMapping("/list/{courseId}")
    public Result<PageResult<GradeAppealVO>> listByCourse(@PathVariable Long courseId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gradeAppealService.listByCourse(courseId, status, page, pageSize));
    }

    @GetMapping("/{appealId}")
    public Result<GradeAppealVO> detail(@PathVariable Long appealId) {
        return Result.success(gradeAppealService.detail(appealId));
    }

    @PostMapping("/review")
    public Result<GradeAppealVO> review(@RequestBody GradeAppealReviewDTO dto) {
        return Result.success(gradeAppealService.review(dto));
    }
}
