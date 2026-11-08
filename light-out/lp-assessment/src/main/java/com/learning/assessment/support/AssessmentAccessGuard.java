package com.learning.assessment.support;

import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.CourseAccessMapper;
import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.exception.UnauthorizedException;
import com.learning.lpcommon.utils.UserContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    private static final String PUBLISH_IMMEDIATE = "immediate";
    private static final String PUBLISH_SCHEDULED = "scheduled";

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
            throw new UnauthorizedException("未登录或登录状态无效，无法" + action);
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

    /** 要求学生已加入一门当前上线的课程。公开课程也必须先选课才能参与考核。 */
    public void requireCourseStudent(Long courseId, String action) {
        requireStudent(action);
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        Long userId = requireUserId(action);
        if (courseAccessMapper.countActiveCourseStudent(courseId, userId) == 0) {
            throw new ForbiddenException("无权" + action + "：您未加入该课程或课程未上线");
        }
    }

    /** 读取课程考核数据：教师校验授课关系，学生校验选课关系。 */
    public void requireCourseAssessmentAccess(Long courseId, String action) {
        if (isTeaching()) {
            requireCourseTeacher(courseId, action);
            return;
        }
        if (isStudent()) {
            requireCourseStudent(courseId, action);
            return;
        }
        throw new ForbiddenException("无权" + action);
    }

    /** 单条任务读取权限；学生还必须等到任务实际发布。 */
    public void requireTaskReadable(Task task, String action) {
        if (task == null) {
            throw new BaseException("任务不存在或已删除");
        }
        requireCourseAssessmentAccess(task.getCourseId(), action);
        if (isStudent() && !isTaskPublished(task)) {
            throw new ForbiddenException("无权" + action + "：任务尚未发布");
        }
    }

    /** 学生开始或提交作答时，同时校验选课、发布时间与截止策略。 */
    public void requireTaskSubmittable(Task task, String action) {
        requireStudent(action);
        requireTaskReadable(task, action);
        LocalDateTime deadline = task.getDeadline();
        boolean allowLate = Integer.valueOf(1).equals(task.getAllowLate());
        if (deadline != null && LocalDateTime.now().isAfter(deadline) && !allowLate) {
            throw new ForbiddenException("无权" + action + "：任务已截止且不允许迟交");
        }
    }

    public boolean isTaskPublished(Task task) {
        if (task == null) {
            return false;
        }
        if (PUBLISH_IMMEDIATE.equals(task.getPublishType())) {
            return true;
        }
        return PUBLISH_SCHEDULED.equals(task.getPublishType())
                && task.getPublishTime() != null
                && !task.getPublishTime().isAfter(LocalDateTime.now());
    }
}
