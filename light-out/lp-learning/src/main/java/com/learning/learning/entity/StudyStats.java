package com.learning.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("study_stats")
public class StudyStats extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("video_duration")
    private Integer videoDuration;

    @TableField("doc_duration")
    private Integer docDuration;

    @TableField("total_duration")
    private Integer totalDuration;
}
