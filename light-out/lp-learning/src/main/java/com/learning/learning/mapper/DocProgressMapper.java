package com.learning.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learning.learning.entity.DocProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocProgressMapper extends BaseMapper<DocProgress> {
    DocProgress selectByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    int upsert(DocProgress entity);
}
