package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.VideoProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoProgressMapper extends BaseMapper<VideoProgress> {
    VideoProgress selectByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    VideoProgress selectLatestByUser(@Param("userId") Long userId);

    int upsert(VideoProgress entity);
}
