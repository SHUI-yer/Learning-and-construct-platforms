package com.learning.learning.service.impl;

import com.learning.learning.dto.AiQaDTO;
import com.learning.learning.mapper.AiQaLogMapper;
import com.learning.learning.vo.AiQaVO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("即时问答 - AiQaService 单元测试")
class AiQaServiceImplTest {

    @Mock private AiQaLogMapper aiQaLogMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiQaServiceImpl aiQaService;

    @Test
    @DisplayName("AI问答 — 应生成Mock回答并记录日志")
    void ask_shouldGenerateAnswerAndSaveLog() {
        AiQaDTO dto = new AiQaDTO();
        dto.setResourceId(100L);
        dto.setQuestion("这段视频讲了什么？");

        when(aiQaLogMapper.insert(org.mockito.ArgumentMatchers.<com.learning.learning.entity.AiQaLog>any())).thenReturn(1);

        AiQaVO vo = aiQaService.ask(1L, dto);

        assertNotNull(vo);
        assertEquals("这段视频讲了什么？", vo.getQuestion());
        assertNotNull(vo.getAnswer(), "应生成AI回答");
        assertFalse(vo.getSourceRefs().isEmpty(), "应包含引用来源");
        assertNotNull(vo.getAskedAt(), "应包含问答时间");
        verify(aiQaLogMapper, times(1)).insert(org.mockito.ArgumentMatchers.<com.learning.learning.entity.AiQaLog>any());
    }

    @Test
    @DisplayName("AI问答 — 无效输入应返回空VO")
    void ask_whenInvalidInput_shouldReturnEmptyVO() {
        AiQaDTO noQuestion = new AiQaDTO();
        noQuestion.setResourceId(100L);

        AiQaVO vo = aiQaService.ask(1L, noQuestion);
        assertNull(vo.getAnswer());

        verify(aiQaLogMapper, never()).insert(org.mockito.ArgumentMatchers.<com.learning.learning.entity.AiQaLog>any());
    }

    @Test
    @DisplayName("AI问答 — sourceRefs应包含引用来源(章节/时间点)")
    void ask_sourceRefs_shouldContainReferences() {
        AiQaDTO dto = new AiQaDTO();
        dto.setResourceId(200L);
        dto.setQuestion("解释这个公式的含义");

        when(aiQaLogMapper.insert(org.mockito.ArgumentMatchers.<com.learning.learning.entity.AiQaLog>any())).thenReturn(1);

        AiQaVO vo = aiQaService.ask(1L, dto);

        assertNotNull(vo.getSourceRefs());
        assertTrue(vo.getSourceRefs().stream().anyMatch(ref -> ref.contains("timestamp")),
            "应引用视频时间点");
    }

    @Test
    @DisplayName("问答历史 — 按用户和资源过滤")
    void getHistory_shouldFilterByUserAndResource() {
        when(aiQaLogMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        var history = aiQaService.getHistory(1L, 100L);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
