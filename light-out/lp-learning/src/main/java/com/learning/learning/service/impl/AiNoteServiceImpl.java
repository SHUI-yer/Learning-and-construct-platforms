package com.learning.learning.service.impl;

import com.learning.learning.dto.NoteDTO;
import com.learning.learning.entity.Note;
import com.learning.learning.mapper.NoteMapper;
import com.learning.learning.service.AiNoteService;
import com.learning.learning.vo.NoteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiNoteServiceImpl implements AiNoteService {

    private final NoteMapper noteMapper;

    @Override
    public NoteVO generateNote(Long userId, NoteDTO dto) {
        if (userId == null || dto == null || dto.getResourceId() == null) {
            return new NoteVO();
        }
        // Mock AI summary: combine start/end and optional user content
        String summary = String.format("[AI Note] summary for resource %d (%ds-%ds): %s",
                dto.getResourceId(), dto.getStartTime(), dto.getEndTime(), dto.getContent() == null ? "auto summary" : dto.getContent());

        Note entity = new Note();
        entity.setUserId(userId);
        entity.setResourceId(dto.getResourceId());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setContent(summary);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        noteMapper.insert(entity);

        NoteVO vo = new NoteVO();
        vo.setNoteId(entity.getId());
        vo.setResourceId(entity.getResourceId());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setContent(entity.getContent());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    @Override
    public List<NoteVO> listNotes(Long userId, Long resourceId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId);
        if (resourceId != null) {
            wrapper.eq(Note::getResourceId, resourceId);
        }
        wrapper.orderByAsc(Note::getStartTime);
        List<Note> notes = noteMapper.selectList(wrapper);
        return notes.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public NoteVO getNote(Long noteId) {
        Note entity = noteMapper.selectById(noteId);
        if (entity == null) {
            return new NoteVO();
        }
        return toVo(entity);
    }

    private NoteVO toVo(Note entity) {
        NoteVO vo = new NoteVO();
        vo.setNoteId(entity.getId());
        vo.setResourceId(entity.getResourceId());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setContent(entity.getContent());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
