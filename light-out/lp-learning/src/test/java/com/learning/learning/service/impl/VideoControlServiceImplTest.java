package com.learning.learning.service.impl;

// 修正：替换为Jackson官方ObjectMapper包，hutool的这个类不要用
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.learning.dto.VideoControlDTO;
import com.learning.learning.entity.VideoControl;
import com.learning.learning.mapper.VideoControlMapper;
import com.learning.learning.vo.VideoControlVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// 规范注解顺序
@ExtendWith(MockitoExtension.class)
@DisplayName("视频防拖拽 - VideoControlService 单元测试")
class VideoControlServiceImplTest {

    // ✅ @Mock全部放到类内部成员变量位置，修正位置错误
    @Mock
    private VideoControlMapper videoControlMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    // ✅ 正确包+正确位置的ObjectMapper Mock
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VideoControlServiceImpl videoControlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(videoControlService, "keyPrefix", "lp");
        // 给objectMapper基础打桩，消除unused警告
    }

    @Test
    @DisplayName("设置视频管控 — 防拖拽+防快进")
    void setControl_shouldSetDragAndFastForward() {
        VideoControlDTO dto = new VideoControlDTO();
        dto.setResourceId(100L);
        dto.setNoDrag(true);
        dto.setNoFastForward(true);
        dto.setMinPlaySpeed(new BigDecimal("0.5"));
        dto.setMaxPlaySpeed(new BigDecimal("2.0"));

        when(videoControlMapper.selectOne(any())).thenReturn(null);
        when(videoControlMapper.insert(any(VideoControl.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        videoControlService.setControl(dto);

        verify(videoControlMapper, times(1)).insert(any(VideoControl.class));
        verify(redisTemplate, times(1)).delete(contains("lp:vc:100"));
    }

    @Test
    @DisplayName("查询视频管控 — 防拖拽/防快进/倍速限制")
    void getControl_shouldReturnAllSettings() {
        VideoControl entity = new VideoControl();
        entity.setResourceId(100L);
        entity.setNoDrag(true);
        entity.setNoFastForward(false);
        entity.setMinPlaySpeed(new BigDecimal("0.5"));
        entity.setMaxPlaySpeed(new BigDecimal("2.0"));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(videoControlMapper.selectOne(any())).thenReturn(entity);

        doNothing().when(valueOperations).set(anyString(), any(), any());

        VideoControlVO vo = videoControlService.getControl(100L);

        assertNotNull(vo);
        assertTrue(vo.getNoDrag(), "应启用防拖拽");
        assertFalse(vo.getNoFastForward(), "应未启用防快进");
        assertEquals(new BigDecimal("0.5"), vo.getMinPlaySpeed(), "最小倍速应为0.5");
        assertEquals(new BigDecimal("2.0"), vo.getMaxPlaySpeed(), "最大倍速应为2.0");
    }

    @Test
    @DisplayName("查询视频管控 — 未配置返回默认空VO")
    void getControl_whenNotConfigured_shouldReturnEmptyVO() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(videoControlMapper.selectOne(any())).thenReturn(null);

        VideoControlVO vo = videoControlService.getControl(100L);

        assertNotNull(vo);
        assertNull(vo.getNoDrag());
    }
}