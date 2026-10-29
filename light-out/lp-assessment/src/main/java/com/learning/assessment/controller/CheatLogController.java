package com.learning.assessment.controller;

import com.learning.assessment.dto.CheatLogRecordDTO;
import com.learning.assessment.service.CheatLogService;
import com.learning.assessment.vo.CheatLogVO;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cheatLog")
public class CheatLogController {

    @Resource
    private CheatLogService cheatLogService;

    // ========== 异常行为记录 ==========

    @PostMapping("/record")
    public Result<List<CheatLogVO>> record(@RequestBody CheatLogRecordDTO dto) {
        return Result.success(cheatLogService.record(dto));
    }

    // ========== 异常记录查询 ==========

    @GetMapping("/list/{submissionId}")
    public Result<List<CheatLogVO>> list(@PathVariable Long submissionId) {
        return Result.success(cheatLogService.listBySubmission(submissionId));
    }

    @GetMapping("/count/{submissionId}")
    public Result<Integer> count(@PathVariable Long submissionId,
                                 @RequestParam(required = false) String cheatType) {
        return Result.success(cheatLogService.countBySubmission(submissionId, cheatType));
    }
}
