package com.learning.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_control")
public class VideoControl extends BaseEntity {

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

    @TableField("resource_id")
    private Long resourceId;

    @TableField("no_drag")
    private Boolean noDrag;

    @TableField("no_fast_forward")
    private Boolean noFastForward;

    @TableField("min_play_speed")
    private BigDecimal minPlaySpeed;

    @TableField("max_play_speed")
    private BigDecimal maxPlaySpeed;
}
