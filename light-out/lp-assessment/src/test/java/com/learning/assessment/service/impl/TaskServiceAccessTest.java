package com.learning.assessment.service.impl;

import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.GradeSchemeMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.lpcommon.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceAccessTest {

    @Mock
    private GradeSchemeMapper gradeSchemeMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private AssessmentAccessGuard accessGuard;
    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void taskDetailRejectsUnauthorizedUser() {
        Task task = task(1L, "immediate");
        when(taskMapper.selectById(1L)).thenReturn(task);
        doThrow(new ForbiddenException("无权查看任务"))
                .when(accessGuard).requireTaskReadable(task, "查看任务");

        assertThrows(ForbiddenException.class, () -> taskService.getTaskById(1L));
    }

    @Test
    void studentTaskListFiltersUnpublishedTasks() {
        Task published = task(1L, "immediate");
        Task unpublished = task(2L, "scheduled");
        unpublished.setPublishTime(LocalDateTime.now().plusHours(1));
        when(taskMapper.selectByCourseId(10L)).thenReturn(List.of(published, unpublished));
        when(accessGuard.isStudent()).thenReturn(true);
        when(accessGuard.isTaskPublished(published)).thenReturn(true);
        when(accessGuard.isTaskPublished(unpublished)).thenReturn(false);

        assertEquals(1, taskService.listTasksByCourse(10L).size());
        verify(accessGuard).requireCourseAssessmentAccess(10L, "查看课程任务");
    }

    private static Task task(Long id, String publishType) {
        return Task.builder()
                .taskId(id)
                .courseId(10L)
                .title("任务" + id)
                .publishType(publishType)
                .build();
    }
}
