package com.learning.lpcourse.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCreateDTO {
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 128, message = "课程名称不能超过128个字符")
    private String courseName;
    @Size(max = 64, message = "课程编码不能超过64个字符")
    private String courseCode;
    @Positive(message = "分院ID必须为正数")
    private Long deptId;
    @NotBlank(message = "学期不能为空")
    @Size(max = 32, message = "学期不能超过32个字符")
    private String semester;
    @NotNull(message = "是否公开不能为空")
    @Min(value = 0, message = "是否公开只能为0或1")
    @Max(value = 1, message = "是否公开只能为0或1")
    private Integer isPublic;
    @Size(max = 512, message = "封面地址不能超过512个字符")
    private String coverUrl;
    @Size(max = 5000, message = "课程描述不能超过5000个字符")
    private String description;
}
