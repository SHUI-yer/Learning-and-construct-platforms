package com.learning.assessment.service;

import com.learning.assessment.dto.QuestionCreateDTO;
import com.learning.assessment.vo.QuestionVO;
import com.learning.lpcommon.result.PageResult;

public interface QuestionBankService {

    QuestionVO createQuestion(QuestionCreateDTO dto);

    QuestionVO updateQuestion(QuestionCreateDTO dto);

    void deleteQuestion(Long qId);

    QuestionVO getQuestionById(Long qId);

    PageResult listQuestions(Long courseId, String qType, Integer difficulty, String keyword,
                             Integer page, Integer pageSize);

    int countByCourseId(Long courseId);
}