package com.learning.learning.service.impl;

import com.learning.learning.entity.StudyStats;
import com.learning.learning.mapper.StudyStatsMapper;
import com.learning.learning.vo.CourseProgressVO;
import com.learning.learning.vo.DashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) //新增这行
@DisplayName("进度跟踪 - StudyStatsService 单元测试")
class StudyStatsServiceImplTest {

    @Mock private StudyStatsMapper studyStatsMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StudyStatsServiceImpl studyStatsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(studyStatsService, "keyPrefix", "lp");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("添加学习时长 — 视频类型应增加视频时长")
    void addDuration_videoType_shouldIncreaseVideoDuration() {
        Map<Object, Object> current = Map.of("videoDuration", "100", "docDuration", "50", "totalDuration", "150");
        when(hashOperations.entries(anyString())).thenReturn(current);
        // ✅ 修复void方法打桩，替换原when写法
        doNothing().when(hashOperations).putAll(anyString(), anyMap());

        studyStatsService.addDuration(1L, 10L, 60, "video");

        verify(hashOperations, times(1)).putAll(anyString(), anyMap());
    }

    @Test
    @DisplayName("添加学习时长 — null入参应直接返回")
    void addDuration_whenNullInput_shouldReturnEarly() {
        studyStatsService.addDuration(null, 10L, 60, "video");
        studyStatsService.addDuration(1L, null, 60, "video");

        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    @DisplayName("查询课程进度 — 应返回视频/文档百分比和总时长")
    void getStats_shouldReturnProgressPercentages() {
        StudyStats entity = new StudyStats();
        entity.setVideoDuration(300);
        entity.setDocDuration(200);
        entity.setTotalDuration(500);

        when(hashOperations.entries(anyString())).thenReturn(java.util.Collections.emptyMap());
        when(studyStatsMapper.selectOne(any())).thenReturn(entity);

        LocalDate today = LocalDate.now();
        CourseProgressVO vo = studyStatsService.getStats(1L, 10L, today);

        assertNotNull(vo);
        assertEquals(new BigDecimal("60.00"), vo.getVideoPercent(), "视频占比60%");
        assertEquals(new BigDecimal("40.00"), vo.getDocPercent(), "文档占比40%");
        assertEquals(500, vo.getTotalDuration());
    }

    @Test
    @DisplayName("查询课程进度 — 无记录返回空VO")
    void getStats_whenNoRecord_shouldReturnEmptyVO() {
        when(hashOperations.entries(anyString())).thenReturn(java.util.Collections.emptyMap());
        when(studyStatsMapper.selectOne(any())).thenReturn(null);

        CourseProgressVO vo = studyStatsService.getStats(1L, 10L, LocalDate.now());

        assertNotNull(vo);
        assertNull(vo.getVideoPercent());
    }

    @Test
    @DisplayName("数据大屏 — 应包含DAU/课程排行/作业完成率")
    void getDashboard_shouldContainAllMetrics() {
        when(studyStatsMapper.countDistinctUsersByDate(anyString())).thenReturn(128L);
        when(studyStatsMapper.countDistinctUsersSince(anyString())).thenReturn(860L);
        when(studyStatsMapper.avgCompletionForCourse(anyLong())).thenReturn(new BigDecimal("65.50"));
        when(studyStatsMapper.courseVisitRanking(anyString(), anyInt())).thenReturn(java.util.Collections.emptyList());
        when(studyStatsMapper.assignmentSubmissionRate(anyLong())).thenReturn(new BigDecimal("82.30"));

        DashboardVO vo = studyStatsService.getDashboard(10L);

        assertNotNull(vo);
        assertEquals(128L, vo.getDau(), "DAU应为128");
        assertEquals(860L, vo.getWeeklyActive(), "周活应为860");
        assertEquals(new BigDecimal("65.50"), vo.getAvgCompletion(), "平均完成率65.50%");
        assertEquals(new BigDecimal("82.30"), vo.getAssignmentSubmissionRate(), "作业提交率82.30%");
    }
}