package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.VideoQuality;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoQualityMapper extends BaseMapper<VideoQuality> {

    List<VideoQuality> selectByResourceId(@Param("resourceId") Long resourceId);
}
