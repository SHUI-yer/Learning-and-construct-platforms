package com.learning.assessment.service.impl;

import com.learning.assessment.dto.SubmissionSubmitDTO;
import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.ExamAnswerMapper;
import com.learning.assessment.mapper.ExamPaperMapper;
import com.learning.assessment.mapper.GradeMapper;
import com.learning.assessment.mapper.PaperQuestionMapper;
import com.learning.assessment.mapper.QuestionBankMapper;
import com.learning.assessment.mapper.SubmissionMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.service.CheatLogService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.lpcommon.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceAccessTest {

    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private CheatLogService cheatLogService;
    @Mock
    private ExamAnswerMapper examAnswerMapper;
    @Mock
    private GradeMapper gradeMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private ExamPaperMapper examPaperMapper;
    @Mock
    private PaperQuestionMapper paperQuestionMapper;
    @Mock
    private QuestionBankMapper questionBankMapper;
    @Mock
    private AssessmentAccessGuard accessGuard;
    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void examStartRejectsUnauthorizedStudentBeforeLoadingPaper() {
        Task task = Task.builder().taskId(1L).courseId(10L).taskType("quiz").build();
        when(taskMapper.selectById(1L)).thenReturn(task);
        doThrow(new ForbiddenException("未加入课程"))
                .when(accessGuard).requireTaskSubmittable(task, "开始作答");

        assertThrows(ForbiddenException.class, () -> submissionService.startExam(1L));

        verify(examPaperMapper, never()).selectByTaskId(1L);
    }

    @Test
    void examSubmissionRejectsUnauthorizedStudentBeforeLoadingPaper() {
        Task task = Task.builder().taskId(1L).courseId(10L).taskType("quiz").build();
        when(taskMapper.selectById(1L)).thenReturn(task);
        doThrow(new ForbiddenException("未加入课程"))
                .when(accessGuard).requireTaskSubmittable(task, "提交答卷");
        SubmissionSubmitDTO.AnswerItem answer = new SubmissionSubmitDTO.AnswerItem();
        answer.setQId(100L);
        answer.setStudentAnswer("A");
        SubmissionSubmitDTO dto = new SubmissionSubmitDTO();
        dto.setTaskId(1L);
        dto.setAnswers(List.of(answer));

        assertThrows(ForbiddenException.class, () -> submissionService.submitQuiz(dto));

        verify(examPaperMapper, never()).selectByTaskId(1L);
    }
}
