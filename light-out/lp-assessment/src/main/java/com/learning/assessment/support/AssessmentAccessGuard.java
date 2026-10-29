package com.learning.assessment.support;

import com.learning.assessment.mapper.CourseAccessMapper;
import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.utils.UserContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 考核模块的角色与数据归属校验。
 *
 * <p>背景：登录拦截器只验证"令牌有效"，不验证"这个人能不能做这件事"。
 * 改造前整个考核模块没有任何角色校验，任何登录学生都能调用批阅、发布成绩、
 * 自批申诉等教师接口。所有写操作与敏感读操作都应先经过这里。</p>
 *
 * <p>归属规则：管理员放行；教师/助教必须是该课程的创建者或 course_teacher 成员；
 * 学生只能访问自己的数据。</p>
 */
@Component
public class AssessmentAccessGuard {

    /** 可以进入教师端的角色 */
    private static final Set<String> TEACHING_ROLES = Set.of(
            RoleConstant.SUPER_ADMIN,
            RoleConstant.DEPT_ADMIN,
            RoleConstant.TEACHER,
            RoleConstant.ASSISTANT);

    private static final Set<String> ADMIN_ROLES = Set.of(
            RoleConstant.SUPER_ADMIN,
            RoleConstant.DEPT_ADMIN);

    private final CourseAccessMapper courseAccessMapper;

    public AssessmentAccessGuard(CourseAccessMapper courseAccessMapper) {
        this.courseAccessMapper = courseAccessMapper;
    }

    /** 当前角色是否属于教师端（管理员/教师/助教） */
    public boolean isTeaching() {
        String role = UserContext.getRoleCode();
        return role != null && TEACHING_ROLES.contains(role);
    }

    /** 当前角色是否为学生 */
    public boolean isStudent() {
        return RoleConstant.STUDENT.equals(UserContext.getRoleCode());
    }

    public boolean isAdmin() {
        String role = UserContext.getRoleCode();
        return role != null && ADMIN_ROLES.contains(role);
    }

    /** 当前登录用户 id；拿不到说明令牌里没有业务载荷，按未登录处理 */
    public Long requireUserId(String action) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ForbiddenException("未登录或登录状态无效，无法" + action);
        }
        return userId;
    }

    /** 要求当前用户是教师端角色 */
    public void requireTeaching(String action) {
        if (!isTeaching()) {
            throw new ForbiddenException("无权" + action + "：该操作仅限管理员、教师或助教");
        }
    }

    /** 要求当前用户是学生 */
    public void requireStudent(String action) {
        if (!isStudent()) {
            throw new ForbiddenException("无权" + action + "：该操作仅限学生本人");
        }
    }

    /**
     * 要求当前用户能管理指定课程：管理员直接放行，教师/助教需属于该课程。
     */
    public void requireCourseTeacher(Long courseId, String action) {
        requireTeaching(action);
        if (isAdmin()) {
            return;
        }
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        Long userId = requireUserId(action);
        Long ownerId = courseAccessMapper.selectCourseOwner(courseId);
        if (ownerId == null) {
            throw new BaseException("课程不存在或已被删除");
        }
        if (userId.equals(ownerId)) {
            return;
        }
        if (courseAccessMapper.countCourseTeacher(courseId, userId) > 0) {
            return;
        }
        throw new ForbiddenException("无权" + action + "：您不是该课程的授课教师");
    }
}
