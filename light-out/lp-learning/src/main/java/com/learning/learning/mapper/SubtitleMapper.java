package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.Subtitle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubtitleMapper extends BaseMapper<Subtitle> {

    List<Subtitle> selectByResourceId(@Param("resourceId") Long resourceId);
}
