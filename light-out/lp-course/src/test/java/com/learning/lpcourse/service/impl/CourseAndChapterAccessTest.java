package com.learning.lpcourse.service.impl;

import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcourse.dto.CourseQueryDTO;
import com.learning.lpcourse.mapper.ChapterMapper;
import com.learning.lpcourse.mapper.ContentResourceMapper;
import com.learning.lpcourse.mapper.CourseAuditMapper;
import com.learning.lpcourse.mapper.CourseMapper;
import com.learning.lpcourse.mapper.CourseStudentMapper;
import com.learning.lpcourse.mapper.CourseTeacherMapper;
import com.learning.lpcourse.mapper.StudentClassMapper;
import com.learning.lpcourse.support.CourseAccessGuard;
import com.learning.lpcourse.vo.CourseVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAndChapterAccessTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ChapterMapper chapterMapper;
    @Mock
    private ContentResourceMapper resourceMapper;
    @Mock
    private CourseAccessGuard accessGuard;
    @InjectMocks
    private ChapterServiceImpl chapterService;
    @Mock
    private CourseAuditMapper courseAuditMapper;
    @Mock
    private CourseTeacherMapper courseTeacherMapper;
    @Mock
    private CourseStudentMapper courseStudentMapper;
    @Mock
    private StudentClassMapper studentClassMapper;
    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void chapterTreeRejectsUnauthorizedCourseBeforeLoadingContent() {
        CourseVO course = new CourseVO();
        course.setCourseId(11L);
        when(courseMapper.selectCourseById(11L)).thenReturn(course);
        doThrow(new ForbiddenException("无权查看课程章节"))
                .when(accessGuard).requireCourseReadable(course, "查看课程章节");

        assertThrows(ForbiddenException.class, () -> chapterService.getChapterTree(11L));

        verify(chapterMapper, never()).selectFlatByCourseId(any());
        verify(resourceMapper, never()).selectByChapterIdList(any());
    }

    @Test
    void courseDetailRejectsUnauthorizedUser() {
        CourseVO course = new CourseVO();
        course.setCourseId(12L);
        when(courseMapper.selectCourseById(12L)).thenReturn(course);
        doThrow(new ForbiddenException("无权查看课程详情"))
                .when(accessGuard).requireCourseReadable(course, "查看课程详情");

        assertThrows(ForbiddenException.class, () -> courseService.getCourseInfo(12L));
    }
}
