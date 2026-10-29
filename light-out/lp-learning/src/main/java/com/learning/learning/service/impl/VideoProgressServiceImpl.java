package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learning.learning.dto.VideoProgressDTO;
import com.learning.learning.entity.VideoProgress;
import com.learning.learning.mapper.VideoProgressMapper;
import com.learning.learning.service.VideoProgressService;
import com.learning.learning.vo.VideoProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VideoProgressServiceImpl implements VideoProgressService {

    private final VideoProgressMapper videoProgressMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${learning.redis.key-prefix:lp}")
    private String keyPrefix;

    @Value("${learning.redis.video-progress-ttl:7d}")
    private String videoProgressTtlRaw;

    @Override
    public void reportProgress(VideoProgressDTO dto, Long userId) {
        if (dto == null || userId == null || dto.getResourceId() == null) {
            return;
        }

        // Simple per-user rate limit: 1 report per second
        String rateKey = String.format("%s:rl:vp:%d", keyPrefix, userId);
        Long counter = redisTemplate.opsForValue().increment(rateKey, 1);
        if (counter != null && counter == 1L) {
            redisTemplate.expire(rateKey, java.time.Duration.ofSeconds(1));
        } else if (counter != null && counter > 1L) {
            // exceeded rate limit - ignore this report
            return;
        }

        Integer pos = dto.getCurrentPosition() == null ? 0 : dto.getCurrentPosition();
        Integer total = dto.getTotalDuration() == null ? 1 : dto.getTotalDuration();
        BigDecimal pct = total <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(pos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("resourceId", dto.getResourceId());
        payload.put("position", pos);
        payload.put("watchPercent", pct);
        payload.put("durationWatched", Math.max(pos, 0));
        payload.put("playCount", 1);
        payload.put("timestamp", System.currentTimeMillis());

        // write to redis for quick reads
        saveProgressToRedis(userId, dto.getResourceId(), payload);

        // publish async message for background processing (study stats & DB persist)
        try {
            rabbitTemplate.convertAndSend("learning.topic", "learning.play.report", payload);
        } catch (Exception ignored) {
            // Do not fail the request if MQ is unavailable; background job will handle eventual consistency
        }
    }

    @Override
    public VideoProgressVO getProgress(Long resourceId, Long userId) {
        if (resourceId == null || userId == null) {
            return new VideoProgressVO();
        }
        String redisKey = buildVideoProgressKey(userId, resourceId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        if (!entries.isEmpty()) {
            return toVo(entries);
        }
        VideoProgress entity = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getResourceId, resourceId));
        if (entity == null) {
            return new VideoProgressVO();
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("position", entity.getLastPosition());
        payload.put("watchPercent", entity.getWatchPercent());
        payload.put("durationWatched", entity.getDurationWatched());
        payload.put("playCount", entity.getPlayCount());
        payload.put("timestamp", System.currentTimeMillis());
        saveProgressToRedis(userId, resourceId, payload);
        return toVoFromPayload(payload);
    }

    @Override
    public void batchFlushToDB() {
        Set<String> keys = redisTemplate.keys(buildPattern("vp"));
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
            VideoProgress entity = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                    .eq(VideoProgress::getUserId, userId)
                    .eq(VideoProgress::getResourceId, resourceId));
            if (entity == null) {
                entity = new VideoProgress();
                entity.setUserId(userId);
                entity.setResourceId(resourceId);
            }
            entity.setLastPosition(asInt(entries.get("pos")));
            entity.setWatchPercent(toBigDecimal(entries.get("pct")));
            entity.setDurationWatched(asInt(entries.get("dur")));
            entity.setPlayCount(asInt(entries.get("cnt")));
            entity.setLastWatchedAt(LocalDateTime.now());
            if (entity.getId() == null) {
                videoProgressMapper.insert(entity);
            } else {
                videoProgressMapper.updateById(entity);
            }
            redisTemplate.delete(key);
        }
    }

    @Override
    public void syncProgressFromReport(java.util.Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        Long userId = getLong(payload, "userId");
        Long resourceId = getLong(payload, "resourceId");
        if (userId == null || resourceId == null) {
            return;
        }
        VideoProgress entity = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getResourceId, resourceId));
        if (entity == null) {
            entity = new VideoProgress();
            entity.setUserId(userId);
            entity.setResourceId(resourceId);
        }
        entity.setLastPosition(getInt(payload, "position"));
        entity.setWatchPercent(toBigDecimal(payload.get("watchPercent")));
        entity.setDurationWatched(getInt(payload, "durationWatched"));
        entity.setPlayCount(getInt(payload, "playCount") + (entity.getPlayCount() == null ? 0 : entity.getPlayCount()));
        entity.setLastWatchedAt(LocalDateTime.now());
        if (entity.getId() == null) {
            videoProgressMapper.insert(entity);
        } else {
            videoProgressMapper.updateById(entity);
        }
    }

    private void saveProgressToRedis(Long userId, Long resourceId, Map<String, Object> payload) {
        String key = buildVideoProgressKey(userId, resourceId);
        Map<String, Object> values = new HashMap<>();
        values.put("pos", payload.getOrDefault("position", 0));
        values.put("pct", payload.getOrDefault("watchPercent", BigDecimal.ZERO));
        values.put("dur", payload.getOrDefault("durationWatched", 0));
        values.put("cnt", payload.getOrDefault("playCount", 1));
        values.put("ts", payload.getOrDefault("timestamp", System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, resolveDuration(videoProgressTtlRaw));
    }

    private VideoProgressVO toVo(Map<?, ?> entries) {
        VideoProgressVO vo = new VideoProgressVO();
        vo.setLastPosition(asInt(entries.get("pos")));
        vo.setWatchPercent(toBigDecimal(entries.get("pct")));
        vo.setDurationWatched(asInt(entries.get("dur")));
        vo.setPlayCount(asInt(entries.get("cnt")));
        return vo;
    }

    private VideoProgressVO toVoFromPayload(Map<String, Object> payload) {
        VideoProgressVO vo = new VideoProgressVO();
        vo.setLastPosition(getInt(payload, "position"));
        vo.setWatchPercent(toBigDecimal(payload.get("watchPercent")));
        vo.setDurationWatched(getInt(payload, "durationWatched"));
        vo.setPlayCount(getInt(payload, "playCount"));
        return vo;
    }

    private String buildVideoProgressKey(Long userId, Long resourceId) {
        return String.format("%s:vp:%d:%d", keyPrefix, userId, resourceId);
    }

    private String buildPattern(String type) {
        return String.format("%s:%s:*", keyPrefix, type);
    }

    private Duration resolveDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return Duration.ofDays(7);
        }
        String value = raw.trim().toLowerCase();
        if (value.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        return Duration.ofDays(7);
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

    private Long getLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer getInt(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
