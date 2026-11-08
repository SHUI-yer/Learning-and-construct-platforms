package com.learning.lpcourse.support;

import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.utils.UserContext;
import com.learning.lpcourse.mapper.CourseStudentMapper;
import com.learning.lpcourse.mapper.CourseTeacherMapper;
import com.learning.lpcourse.vo.CourseVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessGuardTest {

    @Mock
    private CourseTeacherMapper courseTeacherMapper;
    @Mock
    private CourseStudentMapper courseStudentMapper;
    @InjectMocks
    private CourseAccessGuard accessGuard;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void studentCannotReadPrivateCourseWithoutEnrollment() {
        login(7L, "student");
        CourseVO course = course(1L, 2, 0, 99L);
        when(courseStudentMapper.countByCourseAndStudent(1L, 7L)).thenReturn(0);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireCourseReadable(course, "查看课程"));
    }

    @Test
    void studentCanReadOnlinePublicCourse() {
        login(7L, "student");
        CourseVO course = course(1L, 2, 1, 99L);
        when(courseStudentMapper.countByCourseAndStudent(1L, 7L)).thenReturn(0);

        assertDoesNotThrow(() -> accessGuard.requireCourseReadable(course, "查看课程"));
    }

    @Test
    void teacherCannotReadAnotherTeachersCourse() {
        login(8L, "teacher");
        CourseVO course = course(1L, 0, 0, 99L);
        when(courseTeacherMapper.countByCourseAndTeacher(1L, 8L)).thenReturn(0);

        assertThrows(ForbiddenException.class,
                () -> accessGuard.requireCourseReadable(course, "查看课程"));
    }

    @Test
    void assignedAssistantCanReadCourse() {
        login(8L, "assistant");
        CourseVO course = course(1L, 0, 0, 99L);
        when(courseTeacherMapper.countByCourseAndTeacher(1L, 8L)).thenReturn(1);

        assertDoesNotThrow(() -> accessGuard.requireCourseReadable(course, "查看课程"));
    }

    @Test
    void teacherCannotCopyAnotherTeachersPrivateCourse() {
        login(8L, "teacher");
        CourseVO course = course(1L, 2, 0, 99L);
        when(courseTeacherMapper.countByCourseAndTeacher(1L, 8L)).thenReturn(0);

        assertThrows(ForbiddenException.class, () -> accessGuard.requireCourseCopyable(course));
    }

    private static CourseVO course(Long id, Integer status, Integer isPublic, Long teacherId) {
        CourseVO course = new CourseVO();
        course.setCourseId(id);
        course.setStatus(status);
        course.setIsPublic(isPublic);
        course.setTeacherId(teacherId);
        return course;
    }

    private static void login(Long userId, String roleCode) {
        UserContext.setLoginUser(Map.of("userId", userId, "roleCode", roleCode));
    }
}
