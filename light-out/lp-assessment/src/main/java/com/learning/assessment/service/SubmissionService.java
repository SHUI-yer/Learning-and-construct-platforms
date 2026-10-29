package com.learning.assessment.service;

import com.learning.assessment.dto.SubmissionGradeDTO;
import com.learning.assessment.dto.SubmissionExcellentDTO;
import com.learning.assessment.dto.SubmissionSubmitDTO;
import com.learning.assessment.vo.ExamStartVO;
import com.learning.assessment.vo.SubmissionVO;
import com.learning.lpcommon.result.PageResult;

public interface SubmissionService {

    ExamStartVO startExam(Long taskId);

    SubmissionVO submitQuiz(SubmissionSubmitDTO dto);

    PageResult listByTask(Long taskId, Integer page, Integer pageSize, String status);

    SubmissionVO detail(Long submissionId);

    SubmissionVO grade(SubmissionGradeDTO dto);

    SubmissionVO setExcellent(SubmissionExcellentDTO dto);

    PageResult listExcellent(Long taskId, Integer page, Integer pageSize);
}
