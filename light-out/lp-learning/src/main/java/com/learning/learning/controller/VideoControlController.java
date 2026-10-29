package com.learning.learning.controller;

import com.learning.learning.dto.VideoControlDTO;
import com.learning.learning.service.VideoControlService;
import com.learning.learning.vo.VideoControlVO;
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

@Tag(name = "视频管控")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/video/control")
public class VideoControlController {

    private final VideoControlService videoControlService;

    @Operation(summary = "设置视频管控")
    @PostMapping
    public Result<Map<String, Object>> setControl(@RequestBody @Valid VideoControlDTO dto) {
        videoControlService.setControl(dto);
        // 固定成功msg + 空HashMap占位data，满足文档双非空要求
        return Result.success(new HashMap<>(), "视频管控配置设置成功");
    }

    @Operation(summary = "查询视频管控")
    @GetMapping
    public Result<VideoControlVO> getControl(@RequestParam Long resourceId) {
        return Result.success(videoControlService.getControl(resourceId), "查询视频管控配置成功");
    }
}
