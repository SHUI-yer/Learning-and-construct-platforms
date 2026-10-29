package com.learning.learning.controller;

import com.learning.learning.dto.SubtitleDTO;
import com.learning.learning.dto.VideoQualityDTO;
import com.learning.learning.service.VideoEnhancementService;
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

@Tag(name = "视频增强")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/video/enhance")
public class VideoEnhancementController {

    private final VideoEnhancementService videoEnhancementService;

    @Operation(summary = "添加字幕资源")
    @PostMapping("/subtitle")
    public Result<Void> addSubtitle(@RequestBody @Valid SubtitleDTO dto) {
        videoEnhancementService.addSubtitle(dto);
        return Result.success(null, "字幕资源添加成功");
    }

    @Operation(summary = "查询字幕列表")
    @GetMapping("/subtitle")
    public Result<List<SubtitleDTO>> listSubtitles(@RequestParam Long resourceId) {
        List<SubtitleDTO> subtitleList = videoEnhancementService.listSubtitles(resourceId);
        // 传入第二个参数，手动指定成功提示文案，msg不再为null
        return Result.success(subtitleList, "查询字幕列表成功");
    }

    @Operation(summary = "添加清晰度/码率资源")
    @PostMapping("/quality")
    public Result<Void> addQuality(@RequestBody @Valid VideoQualityDTO dto) {
        videoEnhancementService.addQuality(dto);
        return Result.success(null, "清晰度资源添加成功");
    }

    @Operation(summary = "查询清晰度列表")
    @GetMapping("/quality")
    public Result<List<VideoQualityDTO>> listQualities(@RequestParam Long resourceId) {
        return Result.success(videoEnhancementService.listQualities(resourceId), "查询清晰度列表成功");
    }
}
