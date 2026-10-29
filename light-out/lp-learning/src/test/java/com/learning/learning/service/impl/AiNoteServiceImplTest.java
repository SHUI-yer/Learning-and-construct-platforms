package com.learning.learning.service.impl;

import com.learning.learning.dto.NoteDTO;
import com.learning.learning.entity.Note;
import com.learning.learning.mapper.NoteMapper;
import com.learning.learning.vo.NoteVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("智能时间轴笔记 - AiNoteService 单元测试")
class AiNoteServiceImplTest {

    @Mock private NoteMapper noteMapper;

    @InjectMocks
    private AiNoteServiceImpl aiNoteService;

    @Test
    @DisplayName("生成AI笔记 — 应包含AI摘要和时间戳信息")
    void generateNote_shouldCreateAIEnhancedNote() {
        NoteDTO dto = new NoteDTO();
        dto.setResourceId(100L);
        dto.setStartTime(120);
        dto.setEndTime(180);

        when(noteMapper.insert(org.mockito.ArgumentMatchers.<Note>any())).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setId(1L);
            return 1;
        });

        NoteVO vo = aiNoteService.generateNote(1L, dto);

        assertNotNull(vo);
        assertEquals(1L, vo.getNoteId());
        assertEquals(100L, vo.getResourceId());
        assertEquals(120, vo.getStartTime());
        assertEquals(180, vo.getEndTime());
        assertTrue(vo.getContent().contains("[AI Note]"), "应包含AI标记");
        assertTrue(vo.getContent().contains("100"), "应包含资源ID");
        assertNotNull(vo.getCreatedAt());
    }

    @Test
    @DisplayName("生成AI笔记 — userId/dto/resourceId为null应返回空VO")
    void generateNote_whenNullInput_shouldReturnEmptyVO() {
        NoteVO vo1 = aiNoteService.generateNote(null, new NoteDTO());
        NoteVO vo2 = aiNoteService.generateNote(1L, null);

        assertNull(vo1.getNoteId());
        assertNull(vo2.getNoteId());
        verify(noteMapper, never()).insert(org.mockito.ArgumentMatchers.<Note>any());
    }

    @Test
    @DisplayName("查询笔记列表 — 按资源ID过滤并按时间排序")
    void listNotes_shouldFilterAndSort() {
        Note note1 = new Note();
        note1.setId(1L); note1.setResourceId(100L); note1.setStartTime(60); note1.setContent("Note A");
        Note note2 = new Note();
        note2.setId(2L); note2.setResourceId(100L); note2.setStartTime(120); note2.setContent("Note B");

        when(noteMapper.selectList(any())).thenReturn(List.of(note1, note2));

        List<NoteVO> notes = aiNoteService.listNotes(1L, 100L);

        assertEquals(2, notes.size());
        assertEquals("Note A", notes.get(0).getContent());
        assertEquals("Note B", notes.get(1).getContent());
        assertEquals(60, notes.get(0).getStartTime());
        assertEquals(120, notes.get(1).getStartTime());
    }

    @Test
    @DisplayName("查询单条笔记 — 按时间戳点击跳转")
    void getNote_shouldReturnTimestampInfo() {
        Note entity = new Note();
        entity.setId(1L); entity.setResourceId(100L);
        entity.setStartTime(300); entity.setEndTime(360);
        entity.setContent("重要知识点");

        when(noteMapper.selectById(1L)).thenReturn(entity);

        NoteVO vo = aiNoteService.getNote(1L);

        assertNotNull(vo);
        assertEquals(300, vo.getStartTime());  // 支持点击时间戳跳转
        assertEquals(360, vo.getEndTime());
        assertEquals("重要知识点", vo.getContent());
    }
}
