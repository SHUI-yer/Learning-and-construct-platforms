package com.learning.learning.service;

import com.learning.learning.dto.AiQaDTO;
import com.learning.learning.vo.AiQaVO;

import java.util.List;

public interface AiQaService {

    AiQaVO ask(Long userId, AiQaDTO dto);

    List<AiQaVO> getHistory(Long userId, Long resourceId);
}
