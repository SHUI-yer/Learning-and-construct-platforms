package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.VideoControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoControlMapper extends BaseMapper<VideoControl> {
    VideoControl selectByResourceId(@Param("resourceId") Long resourceId);

    int upsert(VideoControl entity);
}
