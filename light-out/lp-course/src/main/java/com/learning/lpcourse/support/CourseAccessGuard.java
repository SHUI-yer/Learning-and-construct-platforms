package com.learning.lpcourse.support;

import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.exception.UnauthorizedException;
import com.learning.lpcommon.utils.UserContext;
import com.learning.lpcourse.mapper.CourseStudentMapper;
import com.learning.lpcourse.mapper.CourseTeacherMapper;
import com.learning.lpcourse.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 课程模块统一的数据范围校验。
 */
@Component
@RequiredArgsConstructor
public class CourseAccessGuard {

    public static final String SCOPE_ADMIN = "admin";
    public static final String SCOPE_TEACHING = "teaching";
    public static final String SCOPE_STUDENT = "student";
    private static final int COURSE_ONLINE = 2;

    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseStudentMapper courseStudentMapper;

    public String currentScope() {
        String role = UserContext.getRoleCode();
        if (RoleConstant.SUPER_ADMIN.equals(role) || RoleConstant.DEPT_ADMIN.equals(role)) {
            return SCOPE_ADMIN;
        }
        if (RoleConstant.TEACHER.equals(role) || RoleConstant.ASSISTANT.equals(role)) {
            return SCOPE_TEACHING;
        }
        if (RoleConstant.STUDENT.equals(role)) {
            return SCOPE_STUDENT;
        }
        throw new ForbiddenException("当前角色无权访问课程数据");
    }

    public Long requireUserId(String action) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录状态无效，无法" + action);
        }
        return userId;
    }

    public void requireCourseReadable(CourseVO course, String action) {
        if (course == null) {
            throw new ForbiddenException("无权" + action);
        }
        String scope = currentScope();
        if (SCOPE_ADMIN.equals(scope)) {
            return;
        }

        Long userId = requireUserId(action);
        if (SCOPE_TEACHING.equals(scope)) {
            if (userId.equals(course.getTeacherId())
                    || courseTeacherMapper.countByCourseAndTeacher(course.getCourseId(), userId) > 0) {
                return;
            }
            throw new ForbiddenException("无权" + action + "：您不是该课程的授课教师");
        }

        boolean online = Integer.valueOf(COURSE_ONLINE).equals(course.getStatus());
        boolean publicCourse = Integer.valueOf(1).equals(course.getIsPublic());
        boolean enrolled = courseStudentMapper.countByCourseAndStudent(course.getCourseId(), userId) > 0;
        if (online && (publicCourse || enrolled)) {
            return;
        }
        throw new ForbiddenException("无权" + action + "：课程未上线、非公开或您尚未选课");
    }

    public void requireCourseCopyable(CourseVO course) {
        if (!SCOPE_TEACHING.equals(currentScope())) {
            throw new ForbiddenException("仅教师或助教可以复制课程");
        }
        Long userId = requireUserId("复制课程");
        boolean assigned = userId.equals(course.getTeacherId())
                || courseTeacherMapper.countByCourseAndTeacher(course.getCourseId(), userId) > 0;
        boolean publicOnline = Integer.valueOf(COURSE_ONLINE).equals(course.getStatus())
                && Integer.valueOf(1).equals(course.getIsPublic());
        if (!assigned && !publicOnline) {
            throw new ForbiddenException("无权复制其他教师的非公开课程");
        }
    }
}
