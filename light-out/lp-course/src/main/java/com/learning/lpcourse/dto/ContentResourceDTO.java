package com.learning.lpcourse.dto;

import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContentResourceDTO {
    @NotNull(groups = ValidationGroups.Update.class, message = "资源ID不能为空")
    @Positive(message = "资源ID必须为正数")
    private Long resourceId;
    @NotNull(message = "章节ID不能为空")
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
    @Size(max = 32, message = "资源类型不能超过32个字符")
    private String resourceType;
    @NotBlank(message = "资源标题不能为空")
    @Size(max = 255, message = "资源标题不能超过255个字符")
    private String title;
    @Size(max = 1024, message = "资源地址不能超过1024个字符")
    private String url;
    @PositiveOrZero(message = "资源时长不能为负数")
    private Integer duration;
    @Size(max = 10000, message = "资源内容不能超过10000个字符")
    private String contentText;
    @PositiveOrZero(message = "排序序号不能为负数")
    private Integer sortOrder;
}
