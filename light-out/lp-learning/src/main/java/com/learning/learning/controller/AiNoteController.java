package com.learning.learning.controller;

import com.learning.learning.dto.NoteDTO;
import com.learning.learning.service.AiNoteService;
import com.learning.learning.vo.NoteVO;
import com.learning.lpcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能时间轴笔记")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/ai/note")
public class AiNoteController {

    private final AiNoteService aiNoteService;

    @Operation(summary = "生成时间轴笔记（AI）")
    @PostMapping("/generate")
    public Result<NoteVO> generate(@RequestParam Long userId, @RequestBody @Valid NoteDTO dto) {
        return Result.success(aiNoteService.generateNote(userId, dto), "生成时间轴笔记（AI）成功");
    }

    @Operation(summary = "查询笔记列表（按资源）")
    @GetMapping("/list")
    public Result<List<NoteVO>> listNotes(@RequestParam Long userId,
                                          @RequestParam(required = false) Long resourceId) {
        return Result.success(aiNoteService.listNotes(userId, resourceId), "查询笔记列表成功");
    }

    @Operation(summary = "查询单条笔记详情（点击时间戳跳转）")
    @GetMapping("/{noteId}")
    public Result<NoteVO> getNote(@PathVariable Long noteId) {
        return Result.success(aiNoteService.getNote(noteId), "查询单条笔记详情成功");
    }
}
