package com.learning.learning.controller;

import com.learning.learning.dto.DocProgressDTO;
import com.learning.learning.service.DocProgressService;
import com.learning.learning.vo.DocProgressVO;
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

import java.math.BigDecimal;

@Tag(name = "文档进度")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/doc/progress")
public class DocProgressController {

    private final DocProgressService docProgressService;

    @Operation(summary = "上报文档阅读进度")
    @PostMapping
    public Result<Void> reportDocProgress(@RequestBody @Valid DocProgressDTO dto,
                                          @RequestParam(value = "userId", required = false) Long userId) {
        docProgressService.reportDocProgress(dto, userId);
        return Result.success();
    }

    @Operation(summary = "查询文档阅读进度")
    @GetMapping
    public Result<DocProgressVO> getDocProgress(@RequestParam Long resourceId,
                                                @RequestParam Long userId) {
        DocProgressVO vo = docProgressService.getDocProgress(resourceId, userId);
        String msg;
        // 判断是否为空进度（两个字段都为0代表暂无进度）
        if (vo.getLastPosition() == 0 && BigDecimal.ZERO.compareTo(vo.getReadPercent()) == 0) {
            msg = "查询成功，暂无该文档阅读进度";
        } else {
            msg = "查询文档阅读进度成功";
        }
        // 参数顺序：Result.success(data, msg)
        return Result.success(vo, msg);
    }
}
