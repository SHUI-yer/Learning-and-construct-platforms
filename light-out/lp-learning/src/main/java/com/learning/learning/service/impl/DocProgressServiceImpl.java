package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learning.learning.dto.DocProgressDTO;
import com.learning.learning.entity.DocProgress;
import com.learning.learning.mapper.DocProgressMapper;
import com.learning.learning.service.DocProgressService;
import com.learning.learning.vo.DocProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocProgressServiceImpl implements DocProgressService {

    private final DocProgressMapper docProgressMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${learning.redis.key-prefix:lp}")
    private String keyPrefix;

    @Override
    public void reportDocProgress(DocProgressDTO dto, Long userId) {
        if (dto == null || userId == null || dto.getResourceId() == null) {
            return;
        }
        Integer position = dto.getCurrentPosition() == null ? 0 : dto.getCurrentPosition();
        Integer totalLength = dto.getTotalLength() == null ? 1 : dto.getTotalLength();
        BigDecimal percent = totalLength <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(position)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLength), 2, java.math.RoundingMode.HALF_UP);
        DocProgress entity = docProgressMapper.selectOne(new LambdaQueryWrapper<DocProgress>()
                .eq(DocProgress::getUserId, userId)
                .eq(DocProgress::getResourceId, dto.getResourceId()));
        if (entity == null) {
            entity = new DocProgress();
            entity.setUserId(userId);
            entity.setResourceId(dto.getResourceId());
        }
        entity.setLastPosition(position);
        entity.setReadPercent(percent);
        entity.setLastReadAt(LocalDateTime.now());
        if (entity.getId() == null) {
            docProgressMapper.insert(entity);
        } else {
            docProgressMapper.updateById(entity);
        }
        Map<String, Object> values = new HashMap<>();
        values.put("pos", position);
        values.put("pct", percent);
        values.put("ts", System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(buildDocProgressKey(userId, dto.getResourceId()), values);
        redisTemplate.expire(buildDocProgressKey(userId, dto.getResourceId()), Duration.ofDays(7));
    }

    @Override
    public DocProgressVO getDocProgress(Long resourceId, Long userId) {
        // 参数为空，返回默认0对象，杜绝null字段
        if (resourceId == null || userId == null) {
            DocProgressVO vo = new DocProgressVO();
            vo.setLastPosition(0);
            vo.setReadPercent(BigDecimal.ZERO);
            return vo;
        }
        String key = buildDocProgressKey(userId, resourceId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        // Redis缓存命中
        if (!entries.isEmpty()) {
            DocProgressVO vo = new DocProgressVO();
            vo.setLastPosition(asInt(entries.get("pos")));
            vo.setReadPercent(toBigDecimal(entries.get("pct")));
            return vo;
        }
        // Redis没有，查询数据库
        DocProgress entity = docProgressMapper.selectOne(new LambdaQueryWrapper<DocProgress>()
                .eq(DocProgress::getUserId, userId)
                .eq(DocProgress::getResourceId, resourceId));

        DocProgressVO vo = new DocProgressVO();
        if (entity != null) {
            // 查到数据库记录，正常赋值
            vo.setLastPosition(entity.getLastPosition());
            vo.setReadPercent(entity.getReadPercent());
        } else {
            // 数据库无进度记录：赋值默认0，不返回null
            vo.setLastPosition(0);
            vo.setReadPercent(BigDecimal.ZERO);
        }
        return vo;
    }

    @Override
    public void batchFlushToDB() {
        Set<String> keys = redisTemplate.keys(String.format("%s:dp:*", keyPrefix));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                continue;
            }
            String[] parts = key.split(":");
            Long userId = parseLong(parts[parts.length - 2]);
            Long resourceId = parseLong(parts[parts.length - 1]);
            DocProgress entity = docProgressMapper.selectOne(new LambdaQueryWrapper<DocProgress>()
                    .eq(DocProgress::getUserId, userId)
                    .eq(DocProgress::getResourceId, resourceId));
            if (entity == null) {
                entity = new DocProgress();
                entity.setUserId(userId);
                entity.setResourceId(resourceId);
            }
            entity.setLastPosition(asInt(entries.get("pos")));
            entity.setReadPercent(toBigDecimal(entries.get("pct")));
            entity.setLastReadAt(LocalDateTime.now());
            if (entity.getId() == null) {
                docProgressMapper.insert(entity);
            } else {
                docProgressMapper.updateById(entity);
            }
            redisTemplate.delete(key);
        }
    }

    private String buildDocProgressKey(Long userId, Long resourceId) {
        return String.format("%s:dp:%d:%d", keyPrefix, userId, resourceId);
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
