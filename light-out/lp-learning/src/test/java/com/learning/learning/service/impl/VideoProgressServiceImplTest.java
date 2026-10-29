package com.learning.learning.service.impl;

import com.learning.learning.dto.VideoProgressDTO;
import com.learning.learning.entity.VideoProgress;
import com.learning.learning.mapper.VideoProgressMapper;
import com.learning.learning.vo.VideoProgressVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) //新增这行
@DisplayName("断点续学 - VideoProgressService 单元测试")
class VideoProgressServiceImplTest {

    @Mock private VideoProgressMapper videoProgressMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private VideoProgressServiceImpl videoProgressService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(videoProgressService, "keyPrefix", "lp");
        ReflectionTestUtils.setField(videoProgressService, "videoProgressTtlRaw", "7d");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("上报视频进度 — 正常数据应写入Redis并发送MQ")
    void reportProgress_shouldWriteToRedisAndSendMQ() {
        // given
        VideoProgressDTO dto = new VideoProgressDTO();
        dto.setResourceId(100L);
        dto.setCurrentPosition(300);
        dto.setTotalDuration(600);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString(), eq(1L))).thenReturn(1L);

        // when
        videoProgressService.reportProgress(dto, 1L);

        // then
        verify(hashOperations, times(1)).putAll(contains("lp:vp:1:100"), anyMap());
        verify(rabbitTemplate, times(1)).convertAndSend(eq("learning.topic"), eq("learning.play.report"), org.mockito.ArgumentMatchers.<java.util.Map<String,Object>>any());
    }

    @Test
    @DisplayName("上报视频进度 — dto/userId为null应直接返回")
    void reportProgress_whenNullInput_shouldReturnEarly() {
        videoProgressService.reportProgress(null, 1L);
        videoProgressService.reportProgress(new VideoProgressDTO(), null);

        verify(redisTemplate, never()).opsForHash();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("查询视频进度 — Redis有缓存应直接返回")
    void getProgress_whenRedisHasData_shouldReturnFromCache() {
        // given
        when(hashOperations.entries(contains("lp:vp:1:100"))).thenReturn(
            java.util.Map.of("pos", 300, "pct", new BigDecimal("50.00"), "dur", 300, "cnt", 2)
        );

        // when
        VideoProgressVO vo = videoProgressService.getProgress(100L, 1L);

        // then
        assertNotNull(vo);
        assertEquals(300, vo.getLastPosition());
        assertEquals(new BigDecimal("50.00"), vo.getWatchPercent());
        assertEquals(300, vo.getDurationWatched());
        assertEquals(2, vo.getPlayCount());
    }

    @Test
    @DisplayName("查询视频进度 — Redis无缓存应从DB查并回写Redis")
    void getProgress_whenRedisMiss_shouldQueryDB() {
        // given
        when(hashOperations.entries(anyString())).thenReturn(java.util.Collections.emptyMap());
        VideoProgress entity = new VideoProgress();
        entity.setId(1L);
        entity.setUserId(1L);
        entity.setResourceId(100L);
        entity.setLastPosition(150);
        entity.setWatchPercent(new BigDecimal("25.00"));
        entity.setDurationWatched(150);
        entity.setPlayCount(1);

        when(videoProgressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        // when
        VideoProgressVO vo = videoProgressService.getProgress(100L, 1L);

        // then
        assertNotNull(vo);
        assertEquals(150, vo.getLastPosition());
        assertEquals(new BigDecimal("25.00"), vo.getWatchPercent());
    }

    @Test
    @DisplayName("查询视频进度 — 无记录返回空VO")
    void getProgress_whenNoRecord_shouldReturnEmptyVO() {
        when(hashOperations.entries(anyString())).thenReturn(java.util.Collections.emptyMap());
        when(videoProgressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        VideoProgressVO vo = videoProgressService.getProgress(100L, 1L);

        assertNotNull(vo);
        assertNull(vo.getLastPosition());
    }

    @Test
    @DisplayName("进度百分比计算 — position=300, total=600, 应为50%")
    void reportProgress_percentCalculation() {
        VideoProgressDTO dto = new VideoProgressDTO();
        dto.setResourceId(100L);
        dto.setCurrentPosition(300);
        dto.setTotalDuration(600);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString(), eq(1L))).thenReturn(1L);

        videoProgressService.reportProgress(dto, 1L);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(eq("learning.topic"), eq("learning.play.report"), captor.capture());
        BigDecimal pct = (BigDecimal) captor.getValue().get("watchPercent");
        assertEquals(new BigDecimal("50.00"), pct);
    }
}
