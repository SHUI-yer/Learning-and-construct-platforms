package com.learning.learning.service;

import com.learning.learning.dto.NoteDTO;
import com.learning.learning.vo.NoteVO;

import java.util.List;

public interface AiNoteService {
    NoteVO generateNote(Long userId, NoteDTO dto);

    List<NoteVO> listNotes(Long userId, Long resourceId);

    NoteVO getNote(Long noteId);
}
