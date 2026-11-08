package com.learning.lpcourse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseQueryDTO {
    @Size(max = 128, message = "课程名称不能超过128个字符")
    private String courseName;
    @Min(value = 0, message = "课程状态必须在0到3之间")
    @Max(value = 3, message = "课程状态必须在0到3之间")
    private Integer status;
    @Positive(message = "分院ID必须为正数")
    private Long deptId;
    @Size(max = 32, message = "学期不能超过32个字符")
    private String semester;
}
