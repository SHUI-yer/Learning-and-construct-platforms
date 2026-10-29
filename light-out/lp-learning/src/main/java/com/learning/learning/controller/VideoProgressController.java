package com.learning.learning.controller;

import com.learning.learning.dto.VideoProgressDTO;
import com.learning.learning.service.VideoProgressService;
import com.learning.learning.vo.VideoProgressVO;
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

import java.util.HashMap;
import java.util.Map;

@Tag(name = "视频进度")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/video/progress")
public class VideoProgressController {
    private final VideoProgressService videoProgressService;


    @Operation(summary = "上报视频进度")
    @PostMapping
    public Result<Map> reportProgress(@RequestBody @Valid VideoProgressDTO dto,
                                      @RequestParam(value = "userId", required = false) Long userId) {
        videoProgressService.reportProgress(dto, userId);
        // 移除错误的msg:标识，正常传参
        return Result.success(new HashMap<>(), "视频学习进度上报成功");
    }

    @Operation(summary = "查询视频进度")
    @GetMapping
    public Result<VideoProgressVO> getProgress(@RequestParam Long resourceId,
                                               @RequestParam Long userId) {
        VideoProgressVO vo = videoProgressService.getProgress(resourceId, userId);
        return Result.success(vo, "查询视频学习进度成功");
    }
}
