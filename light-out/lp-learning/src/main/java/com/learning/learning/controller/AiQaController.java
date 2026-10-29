package com.learning.learning.controller;

import com.learning.learning.dto.AiQaDTO;
import com.learning.learning.service.AiQaService;
import com.learning.learning.vo.AiQaVO;
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

import java.util.List;

@Tag(name = "AI问答")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/ai")
public class AiQaController {

    private final AiQaService aiQaService;

    @Operation(summary = "即时问答")
    @PostMapping("/ask")
    public Result<AiQaVO> ask(@RequestParam Long userId, @RequestBody @Valid AiQaDTO dto) {
        // 补充自定义成功文案，解决msg为null问题
        return Result.success(aiQaService.ask(userId, dto), "即时问答提问成功");
    }

    @Operation(summary = "查询问答历史")
    @GetMapping("/history")
    public Result<List<AiQaVO>> getHistory(@RequestParam Long userId,
                                           @RequestParam(required = false) Long resourceId) {
        // 补充自定义成功文案，解决msg为null问题
        return Result.success(aiQaService.getHistory(userId, resourceId), "查询问答历史记录成功");
    }
}
