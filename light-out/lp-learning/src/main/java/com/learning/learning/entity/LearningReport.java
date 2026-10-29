package com.learning.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_report")
public class LearningReport extends BaseEntity {

    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    @TableField("report_type")
    private String reportType;

    @TableField("period")
    private String period;

    @TableField("data")
    private String data;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @Override
    public Long getId() {
        return reportId;
    }

    @Override
    public void setId(Long id) {
        this.reportId = id;
    }

    @TableField("created_at")
    private LocalDateTime createdAt;
}
