package com.learning.learning.service.impl;

import com.learning.learning.entity.LearningReport;
import com.learning.learning.mapper.DocProgressMapper;
import com.learning.learning.mapper.LearningReportMapper;
import com.learning.learning.mapper.VideoProgressMapper;
import com.learning.learning.vo.LearningReportVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("进度跟踪 - LearningReportService 单元测试")
class LearningReportServiceImplTest {

    @Mock private LearningReportMapper learningReportMapper;
    @Mock private VideoProgressMapper videoProgressMapper;
    @Mock private DocProgressMapper docProgressMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LearningReportServiceImpl learningReportService;

    @Test
    @DisplayName("生成学习报告 — 应汇总视频/文档学习数据")
    void generateReport_shouldAggregateLearningData() {
        when(videoProgressMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(docProgressMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(learningReportMapper.insert(org.mockito.ArgumentMatchers.<LearningReport>any())).thenReturn(1);

        LearningReportVO vo = learningReportService.generateReport(1L, 10L, "weekly", "week");

        assertNotNull(vo);
        assertEquals("weekly", vo.getReportType());
        assertEquals("week", vo.getPeriod());
        assertNotNull(vo.getData());
        assertNotNull(vo.getGeneratedAt());
    }

    @Test
    @DisplayName("生成学习报告 — null入参返回空VO")
    void generateReport_whenNullInput_shouldReturnEmptyVO() {
        LearningReportVO vo = learningReportService.generateReport(null, 10L, "weekly", "week");
        assertNull(vo.getReportId());

        verify(learningReportMapper, never()).insert(org.mockito.ArgumentMatchers.<LearningReport>any());
    }

    @Test
    @DisplayName("查询已有报告 — 先查已有避免重复生成")
    void getOrGenerateReport_shouldReturnExistingFirst() {
        LearningReport existing = new LearningReport();
        existing.setReportId(99L);
        existing.setReportType("weekly");
        existing.setPeriod("week");
        existing.setData("{\"courseId\":10}");

        when(learningReportMapper.selectByUserCourseTypePeriod(eq(1L), eq(10L), eq("weekly"), eq("week")))
                .thenReturn(existing);

        LearningReportVO vo = learningReportService.getOrGenerateReport(1L, 10L, "weekly", "week");

        assertNotNull(vo);
        assertEquals(99L, vo.getReportId());
        assertEquals("weekly", vo.getReportType());
        verify(learningReportMapper, never()).insert(org.mockito.ArgumentMatchers.<LearningReport>any());  // 不应生成新报告
    }

    @Test
    @DisplayName("查询已有报告 — 无已有报告则自动生成")
    void getOrGenerateReport_whenNoExisting_shouldGenerate() {
        when(learningReportMapper.selectByUserCourseTypePeriod(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(null);
        when(videoProgressMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(docProgressMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(learningReportMapper.insert(org.mockito.ArgumentMatchers.<LearningReport>any())).thenReturn(1);

        LearningReportVO vo = learningReportService.getOrGenerateReport(1L, 10L, "weekly", "week");

        assertNotNull(vo);
        verify(learningReportMapper, times(1)).insert(org.mockito.ArgumentMatchers.<LearningReport>any());  // 应生成新报告
    }

    @Test
    @DisplayName("按ID查询报告 — JSON数据应正确反序列化")
    void getReport_shouldDeserializeData() {
        LearningReport entity = new LearningReport();
        entity.setReportId(1L);
        entity.setReportType("monthly");
        entity.setPeriod("month");
        entity.setData("{\"courseId\":10,\"videoProgressCount\":5}");

        when(learningReportMapper.selectById(1L)).thenReturn(entity);

        LearningReportVO vo = learningReportService.getReport(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getReportId());
        assertEquals("monthly", vo.getReportType());
        assertNotNull(vo.getData());
        assertEquals(10, vo.getData().get("courseId"));
    }
}
