package com.learning.assessment.service;

import com.learning.assessment.dto.CheatLogRecordDTO;
import com.learning.assessment.vo.CheatLogVO;

import java.util.List;

public interface CheatLogService {

    List<CheatLogVO> record(CheatLogRecordDTO dto);

    List<CheatLogVO> listBySubmission(Long submissionId);

    int countBySubmission(Long submissionId, String cheatType);

    void recordOverTime(Long submissionId);
}
