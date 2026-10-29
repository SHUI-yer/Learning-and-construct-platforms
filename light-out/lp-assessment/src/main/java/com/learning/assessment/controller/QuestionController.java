package com.learning.assessment.controller;

import com.learning.assessment.dto.QuestionCreateDTO;
import com.learning.assessment.service.QuestionBankService;
import com.learning.assessment.vo.QuestionVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Resource
    private QuestionBankService questionBankService;

    @PostMapping("/create")
    public Result<QuestionVO> createQuestion(@RequestBody QuestionCreateDTO dto) {
        return Result.success(questionBankService.createQuestion(dto));
    }

    @PutMapping("/update")
    public Result<QuestionVO> updateQuestion(@RequestBody QuestionCreateDTO dto) {
        return Result.success(questionBankService.updateQuestion(dto));
    }

    @DeleteMapping("/{qId}")
    public Result<String> deleteQuestion(@PathVariable Long qId) {
        questionBankService.deleteQuestion(qId);
        return Result.success("删除成功");
    }

    @GetMapping("/{qId}")
    public Result<QuestionVO> getQuestionById(@PathVariable Long qId) {
        return Result.success(questionBankService.getQuestionById(qId));
    }

    @GetMapping("/list")
    public Result<PageResult> listQuestions(@RequestParam Long courseId,
                                            @RequestParam(required = false) String qType,
                                            @RequestParam(required = false) Integer difficulty,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(questionBankService.listQuestions(courseId, qType, difficulty, keyword, page, pageSize));
    }

    @GetMapping("/count/{courseId}")
    public Result<Integer> countByCourseId(@PathVariable Long courseId) {
        return Result.success(questionBankService.countByCourseId(courseId));
    }
}