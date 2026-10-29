package com.learning.assessment.service.impl;

import com.learning.assessment.dto.GradeAppealCreateDTO;
import com.learning.assessment.dto.GradeAppealReviewDTO;
import com.learning.assessment.entity.Grade;
import com.learning.assessment.entity.GradeAppeal;
import com.learning.assessment.entity.GradeSummary;
import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.GradeAppealMapper;
import com.learning.assessment.mapper.GradeMapper;
import com.learning.assessment.mapper.GradeSummaryMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.service.GradeAppealService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.GradeAppealVO;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class GradeAppealServiceImpl implements GradeAppealService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final Set<String> STATUS_FILTERS = Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED);

    @Resource
    private GradeAppealMapper gradeAppealMapper;
    @Resource
    private GradeMapper gradeMapper;
    @Resource
    private AssessmentAccessGuard accessGuard;
    @Resource
    private GradeSummaryMapper gradeSummaryMapper;
    @Resource
    private TaskMapper taskMapper;

    // ========== 学生申诉 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradeAppealVO createAppeal(GradeAppealCreateDTO dto) {
        accessGuard.requireStudent("发起申诉");
        if (dto == null || dto.getGradeId() == null) {
            throw new BaseException("成绩记录ID不能为空");
        }
        if (dto.getAppealReason() == null || dto.getAppealReason().isBlank()) {
            throw new BaseException("申诉理由不能为空");
        }
        Grade grade = gradeMapper.selectById(dto.getGradeId());
        if (grade == null) {
            throw new BaseException("成绩记录不存在");
        }
        if (grade.getIsConfirmed() == null || grade.getIsConfirmed() != 1) {
            throw new BaseException("该成绩尚未批阅确认，无法申诉");
        }
        Long studentId = currentUserId();
        if (!studentId.equals(grade.getStudentId())) {
            throw new ForbiddenException("只能申诉自己的成绩");
        }
        boolean hasPending = gradeAppealMapper.selectByGradeId(grade.getGradeId()).stream()
                .anyMatch(a -> STATUS_PENDING.equals(a.getStatus()));
        if (hasPending) {
            throw new BaseException("该成绩已有待处理的申诉");
        }

        GradeAppeal appeal = new GradeAppeal();
        appeal.setGradeId(grade.getGradeId());
        appeal.setStudentId(studentId);
        appeal.setAppealReason(dto.getAppealReason().trim());
        appeal.setStatus(STATUS_PENDING);
        gradeAppealMapper.insert(appeal);
        return buildVO(gradeAppealMapper.selectById(appeal.getAppealId()), grade);
    }

    // ========== 申诉列表 ==========

    @Override
    public PageResult<GradeAppealVO> listMyAppeals(Integer page, Integer pageSize) {
        accessGuard.requireStudent("查看我的申诉");
        Long studentId = currentUserId();
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        long total = gradeAppealMapper.countByStudentId(studentId);
        List<GradeAppeal> list = gradeAppealMapper.selectPageByStudentId(studentId, (p - 1) * ps, ps);
        return PageResult.build(buildVOList(list), total, (long) p, (long) ps);
    }

    // ========== 按提交ID反查成绩ID ==========

    @Override
    public Long getGradeIdBySubmission(Long submissionId) {
        if (submissionId == null) {
            throw new BaseException("提交ID不能为空");
        }
        Grade grade = gradeMapper.selectBySubmissionId(submissionId);
        return grade == null ? null : grade.getGradeId();
    }

    @Override
    public PageResult<GradeAppealVO> listByCourse(Long courseId, String status, Integer page, Integer pageSize) {
        accessGuard.requireTeaching("查看课程申诉");
        accessGuard.requireCourseTeacher(courseId, "查看课程申诉");
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        if (status != null && !status.isBlank() && !STATUS_FILTERS.contains(status)) {
            throw new BaseException("状态筛选仅支持 pending/approved/rejected");
        }
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        String st = (status == null || status.isBlank()) ? null : status;

        long total = gradeAppealMapper.countByCourseId(courseId, st);
        List<GradeAppeal> list = gradeAppealMapper.selectPageByCourseId(courseId, st, (p - 1) * ps, ps);
        return PageResult.build(buildVOList(list), total, (long) p, (long) ps);
    }

    @Override
    public GradeAppealVO detail(Long appealId) {
        if (appealId == null) {
            throw new BaseException("申诉ID不能为空");
        }
        GradeAppeal appeal = gradeAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BaseException("申诉记录不存在");
        }
        Grade grade = gradeMapper.selectById(appeal.getGradeId());
        if (accessGuard.isTeaching()) {
            accessGuard.requireCourseTeacher(grade == null ? null : grade.getCourseId(), "查看申诉详情");
        } else {
            Long currentUserId = accessGuard.requireUserId("查看申诉详情");
            if (!currentUserId.equals(appeal.getStudentId())) {
                throw new ForbiddenException("无权查看他人的申诉");
            }
        }
        return buildVO(appeal, grade);
    }

    // ========== 教师复核 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradeAppealVO review(GradeAppealReviewDTO dto) {
        accessGuard.requireTeaching("复核申诉");
        if (dto == null || dto.getAppealId() == null) {
            throw new BaseException("申诉ID不能为空");
        }
        if (dto.getStatus() == null || (!STATUS_APPROVED.equals(dto.getStatus()) && !STATUS_REJECTED.equals(dto.getStatus()))) {
            throw new BaseException("处理状态仅支持 approved/rejected");
        }
        if (dto.getVersion() == null) {
            throw new BaseException("版本号不能为空");
        }
        GradeAppeal appeal = gradeAppealMapper.selectById(dto.getAppealId());
        if (appeal == null) {
            throw new BaseException("申诉记录不存在");
        }
        if (!STATUS_PENDING.equals(appeal.getStatus())) {
            throw new BaseException("该申诉已处理，不能重复复核");
        }
        Grade grade = gradeMapper.selectById(appeal.getGradeId());
        if (grade == null) {
            throw new BaseException("成绩记录不存在");
        }
        Task task = taskMapper.selectById(grade.getTaskId());
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        accessGuard.requireCourseTeacher(task.getCourseId(), "复核申诉");

        if (STATUS_APPROVED.equals(dto.getStatus())) {
            if (dto.getNewScore() == null) {
                throw new BaseException("同意改分时必须填写新分数");
            }
            BigDecimal max = task.getMaxScore() == null ? BigDecimal.ZERO : task.getMaxScore();
            if (dto.getNewScore().compareTo(BigDecimal.ZERO) < 0 || dto.getNewScore().compareTo(max) > 0) {
                throw new BaseException("新分数必须在0~" + max + "之间");
            }
            // 联动更新 grade：得分 + 加权分
            grade.setScore(scale2(dto.getNewScore()));
            grade.setWeightedScore(weighted(grade.getScore(), task.getWeight()));
            int rows = gradeMapper.update(grade);
            if (rows == 0) {
                throw new BaseException("成绩更新冲突，请重试");
            }
            // 联动 grade_summary：重算总分，已确认则清空确认信息与排名（需重新确认）
            refreshSummary(grade);
        }

        appeal.setTeacherResponse(dto.getTeacherResponse());
        appeal.setNewScore(STATUS_APPROVED.equals(dto.getStatus()) ? scale2(dto.getNewScore()) : null);
        appeal.setStatus(dto.getStatus());
        appeal.setResolvedAt(LocalDateTime.now());
        appeal.setVersion(dto.getVersion());
        int rows = gradeAppealMapper.update(appeal);
        if (rows == 0) {
            throw new BaseException("申诉已被他人处理，请刷新后重试");
        }
        return buildVO(gradeAppealMapper.selectById(appeal.getAppealId()), gradeMapper.selectById(appeal.getGradeId()));
    }

    // ========== 辅助 ==========

    private void refreshSummary(Grade grade) {
        GradeSummary summary = gradeSummaryMapper.selectByCourseAndStudent(grade.getCourseId(), grade.getStudentId());
        if (summary == null) {
            return;
        }
        BigDecimal finalScore = scale2(gradeMapper.sumWeightedScoreByCourseAndStudent(grade.getCourseId(), grade.getStudentId()));
        summary.setFinalScore(finalScore);
        if (summary.getConfirmedAt() != null) {
            // 复核改分后需重新确认，清空确认信息与排名
            summary.setConfirmedAt(null);
            summary.setConfirmedBy(null);
            summary.setRankInCourse(null);
        }
        int rows = gradeSummaryMapper.update(summary);
        if (rows == 0) {
            throw new BaseException("成绩汇总同步失败，请重试");
        }
    }

    private List<GradeAppealVO> buildVOList(List<GradeAppeal> appeals) {
        return appeals.stream()
                .map(a -> buildVO(a, gradeMapper.selectById(a.getGradeId())))
                .toList();
    }

    private GradeAppealVO buildVO(GradeAppeal a, Grade grade) {
        Task task = grade == null ? null : taskMapper.selectById(grade.getTaskId());
        return GradeAppealVO.fromEntity(a, grade, task);
    }

    private BigDecimal weighted(BigDecimal score, BigDecimal weight) {
        BigDecimal w = weight == null ? BigDecimal.ZERO : weight;
        return score.multiply(w).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 不再用 0 占位：0 不是有效用户，会污染申诉的 student_id
            throw new ForbiddenException("未登录或登录状态无效");
        }
        return userId;
    }
}
