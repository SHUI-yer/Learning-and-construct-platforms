package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.learning.dto.VideoControlDTO;
import com.learning.learning.entity.VideoControl;
import com.learning.learning.mapper.VideoControlMapper;
import com.learning.learning.service.VideoControlService;
import com.learning.learning.vo.VideoControlVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VideoControlServiceImpl implements VideoControlService {

    private final VideoControlMapper videoControlMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${learning.redis.key-prefix:lp}")
    private String keyPrefix;

    @Override
    public void setControl(VideoControlDTO dto) {
        if (dto == null || dto.getResourceId() == null) {
            return;
        }
        VideoControl entity = videoControlMapper.selectOne(new LambdaQueryWrapper<VideoControl>()
                .eq(VideoControl::getResourceId, dto.getResourceId()));
        if (entity == null) {
            entity = new VideoControl();
            entity.setResourceId(dto.getResourceId());
        }
        entity.setNoDrag(dto.getNoDrag());
        entity.setNoFastForward(dto.getNoFastForward());
        entity.setMinPlaySpeed(dto.getMinPlaySpeed());
        entity.setMaxPlaySpeed(dto.getMaxPlaySpeed());
        if (entity.getId() == null) {
            videoControlMapper.insert(entity);
        } else {
            videoControlMapper.updateById(entity);
        }
        redisTemplate.delete(buildControlKey(dto.getResourceId()));
    }

    @Override
    public VideoControlVO getControl(Long resourceId) {
        // 统一初始化带默认值的VO，杜绝null字段
        VideoControlVO defaultVo = new VideoControlVO();
        // 给4个管控字段赋业务默认值
        defaultVo.setNoDrag(false);
        defaultVo.setNoFastForward(false);
        defaultVo.setMinPlaySpeed(new BigDecimal("0.5"));
        defaultVo.setMaxPlaySpeed(new BigDecimal("2.0"));

        if (resourceId == null) {
            // 入参为空返回默认配置VO
            return defaultVo;
        }
        String key = buildControlKey(resourceId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof String str) {
            try {
                return objectMapper.readValue(str, VideoControlVO.class);
            } catch (JsonProcessingException ignored) {
                // 序列化异常走库查询兜底
            }
        }
        VideoControl entity = videoControlMapper.selectOne(new LambdaQueryWrapper<VideoControl>()
                .eq(VideoControl::getResourceId, resourceId));
        if (entity == null) {
            // 库无数据返回默认VO
            return defaultVo;
        }
        // 正常映射数据库数据
        VideoControlVO vo = new VideoControlVO();
        vo.setNoDrag(entity.getNoDrag());
        vo.setNoFastForward(entity.getNoFastForward());
        vo.setMinPlaySpeed(entity.getMinPlaySpeed());
        vo.setMaxPlaySpeed(entity.getMaxPlaySpeed());
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), Duration.ofMinutes(10));
        } catch (JsonProcessingException ignored) {
        }
        return vo;
    }

    private String buildControlKey(Long resourceId) {
        return String.format("%s:vc:%d", keyPrefix, resourceId);
    }
}
