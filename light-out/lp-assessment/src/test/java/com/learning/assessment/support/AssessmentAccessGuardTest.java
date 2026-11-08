package com.learning.assessment.support;

import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.CourseAccessMapper;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentAccessGuardTest {

    @Mock
    private CourseAccessMapper courseAccessMapper;
    @InjectMocks
    private AssessmentAccessGuard accessGuard;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void studentCannotReadTaskFromUnenrolledCourse() {
        login(7L, "student");
        Task task = immediateTask();
        when(courseAccessMapper.countActiveCourseStudent(10L, 7L)).thenReturn(0);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireTaskReadable(task, "查看任务"));
    }

    @Test
    void studentCannotReadScheduledTaskBeforePublishTime() {
        login(7L, "student");
        Task task = immediateTask();
        task.setPublishType("scheduled");
        task.setPublishTime(LocalDateTime.now().plusHours(1));
        when(courseAccessMapper.countActiveCourseStudent(10L, 7L)).thenReturn(1);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireTaskReadable(task, "查看任务"));
    }

    @Test
    void studentCannotSubmitAfterDeadlineWhenLateSubmissionDisabled() {
        login(7L, "student");
        Task task = immediateTask();
        task.setDeadline(LocalDateTime.now().minusMinutes(1));
        task.setAllowLate(0);
        when(courseAccessMapper.countActiveCourseStudent(10L, 7L)).thenReturn(1);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireTaskSubmittable(task, "提交答卷"));
    }

    @Test
    void enrolledStudentCanSubmitLateWhenTaskAllowsIt() {
        login(7L, "student");
        Task task = immediateTask();
        task.setDeadline(LocalDateTime.now().minusMinutes(1));
        task.setAllowLate(1);
        when(courseAccessMapper.countActiveCourseStudent(10L, 7L)).thenReturn(1);

        assertDoesNotThrow(() -> accessGuard.requireTaskSubmittable(task, "提交答卷"));
    }

    @Test
    void teacherCannotReadTaskFromAnotherCourse() {
        login(8L, "teacher");
        Task task = immediateTask();
        when(courseAccessMapper.selectCourseOwner(10L)).thenReturn(99L);
        when(courseAccessMapper.countCourseTeacher(10L, 8L)).thenReturn(0);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireTaskReadable(task, "查看任务"));
    }

    private static Task immediateTask() {
        return Task.builder()
                .taskId(1L)
                .courseId(10L)
                .publishType("immediate")
                .deadline(LocalDateTime.now().plusHours(1))
                .allowLate(0)
                .build();
    }

    private static void login(Long userId, String roleCode) {
        UserContext.setLoginUser(Map.of("userId", userId, "roleCode", roleCode));
    }
}
