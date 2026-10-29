package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.AiQaLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiQaLogMapper extends BaseMapper<AiQaLog> {
    List<AiQaLog> selectRecentByUserResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId, @Param("limit") int limit);
}
