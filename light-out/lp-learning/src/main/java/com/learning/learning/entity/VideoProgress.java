package com.learning.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_progress")
public class VideoProgress extends BaseEntity {

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

    @TableField("resource_id")
    private Long resourceId;

    @TableField("last_position")
    private Integer lastPosition;

    @TableField("watch_percent")
    private BigDecimal watchPercent;

    @TableField("duration_watched")
    private Integer durationWatched;

    @TableField("play_count")
    private Integer playCount;

    @TableField("last_watched_at")
    private LocalDateTime lastWatchedAt;
    
    
}
