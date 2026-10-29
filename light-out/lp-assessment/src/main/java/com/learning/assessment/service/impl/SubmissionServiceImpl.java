package com.learning.assessment.service.impl;

import com.learning.assessment.dto.SubmissionGradeDTO;
import com.learning.assessment.dto.SubmissionExcellentDTO;
import com.learning.assessment.dto.SubmissionSubmitDTO;
import com.learning.assessment.entity.ExamAnswer;
import com.learning.assessment.entity.ExamPaper;
import com.learning.assessment.entity.Grade;
import com.learning.assessment.entity.PaperQuestion;
import com.learning.assessment.entity.QuestionBank;
import com.learning.assessment.entity.Submission;
import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.ExamAnswerMapper;
import com.learning.assessment.mapper.ExamPaperMapper;
import com.learning.assessment.mapper.GradeMapper;
import com.learning.assessment.mapper.PaperQuestionMapper;
import com.learning.assessment.mapper.QuestionBankMapper;
import com.learning.assessment.mapper.SubmissionMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.service.CheatLogService;
import com.learning.assessment.service.QuestionValidator;
import com.learning.assessment.service.SubmissionService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.ExamStartVO;
import com.learning.assessment.vo.ExamAnswerVO;
import com.learning.assessment.vo.SubmissionVO;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final String SUBMIT_TYPE_QUIZ = "quiz";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_GRADED = "graded";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final Set<String> STATUS_FILTERS = Set.of(STATUS_PENDING, STATUS_GRADED, STATUS_IN_PROGRESS);

    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private CheatLogService cheatLogService;
    @Resource
    private ExamAnswerMapper examAnswerMapper;
    @Resource
    private GradeMapper gradeMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ExamPaperMapper examPaperMapper;
    @Resource
    private PaperQuestionMapper paperQuestionMapper;
    @Resource
    private QuestionBankMapper questionBankMapper;
    @Resource
    private AssessmentAccessGuard accessGuard;

    // ========== 限时作答 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamStartVO startExam(Long taskId) {
        accessGuard.requireStudent("开始作答");
        if (taskId == null) {
            throw new BaseException("任务ID不能为空");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        if (!SUBMIT_TYPE_QUIZ.equals(task.getTaskType())) {
            throw new BaseException("仅测验/考试任务支持限时作答");
        }
        ExamPaper paper = examPaperMapper.selectByTaskId(taskId);
        if (paper == null) {
            throw new BaseException("任务尚未关联试卷，无法作答");
        }
        if (!isTimedExam(paper)) {
            throw new BaseException("该试卷未设置限时（exam_paper.duration），无需开始作答");
        }
        if (paperQuestionMapper.selectByPaperId(paper.getPaperId()).isEmpty()) {
            throw new BaseException("试卷未配置题目，无法作答");
        }

        Long studentId = currentUserId();
        Submission submission = submissionMapper.selectByTaskAndStudent(taskId, studentId);
        if (submission == null) {
            submission = new Submission();
            submission.setTaskId(taskId);
            submission.setStudentId(studentId);
            submission.setSubmitType(SUBMIT_TYPE_QUIZ);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setIsLate(isLate(task));
            submission.setStatus(STATUS_IN_PROGRESS);
            submissionMapper.insert(submission);
            return buildStartVO(submission, paper);
        }
        if (STATUS_IN_PROGRESS.equals(submission.getStatus())) {
            // 已在进行中，幂等返回，不重置计时
            return buildStartVO(submission, paper);
        }
        if (!isResubmitAllowed(task)) {
            throw new BaseException("该任务已提交且不允许重交");
        }
        // 允许重交：重置为进行中并重新计时
        int rows = submissionMapper.resetToInProgress(submission.getSubmissionId(), studentId, LocalDateTime.now());
        if (rows == 0) {
            throw new BaseException("开始作答失败，请重试");
        }
        examAnswerMapper.deleteBySubmissionId(submission.getSubmissionId());
        return buildStartVO(submissionMapper.selectById(submission.getSubmissionId()), paper);
    }

    // ========== quiz 测验作答 + 自动判分 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmissionVO submitQuiz(SubmissionSubmitDTO dto) {
        accessGuard.requireStudent("提交答卷");
        if (dto == null || dto.getTaskId() == null) {
            throw new BaseException("任务ID不能为空");
        }
        if (dto.getAnswers() == null || dto.getAnswers().isEmpty()) {
            throw new BaseException("作答内容不能为空");
        }
        Task task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        if (!"quiz".equals(task.getTaskType())) {
            throw new BaseException("仅测验/考试任务支持在线作答");
        }
        ExamPaper paper = examPaperMapper.selectByTaskId(task.getTaskId());
        if (paper == null) {
            throw new BaseException("任务尚未关联试卷，无法作答");
        }
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectByPaperId(paper.getPaperId());
        if (paperQuestions.isEmpty()) {
            throw new BaseException("试卷未配置题目，无法作答");
        }
        Map<Long, PaperQuestion> pqByQId = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQId, p -> p, (a, b) -> a));

        Long studentId = currentUserId();
        Submission submission = submissionMapper.selectByTaskAndStudent(task.getTaskId(), studentId);
        if (isTimedExam(paper)) {
            if (submission == null || !STATUS_IN_PROGRESS.equals(submission.getStatus())) {
                throw new BaseException("限时作答需先调用 /submission/start 开始作答");
            }
            assertWithinDuration(submission, paper);
        }
        if (submission != null && !STATUS_IN_PROGRESS.equals(submission.getStatus()) && !isResubmitAllowed(task)) {
            throw new BaseException("该任务已提交且不允许重交");
        }

        List<ExamAnswer> answers = gradeAnswers(dto.getAnswers(), pqByQId);

        if (submission == null) {
            submission = new Submission();
            submission.setTaskId(task.getTaskId());
            submission.setStudentId(studentId);
            submission.setSubmitType(SUBMIT_TYPE_QUIZ);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setIsLate(isLate(task));
            submission.setStatus(STATUS_GRADED);
            submission.setScore(totalScore(answers));
            submissionMapper.insert(submission);
        } else {
            // 重交/限时收卷：覆盖旧作答与历史批阅结果，重新自动判分
            submission.setStatus(STATUS_GRADED);
            submission.setScore(totalScore(answers));
            submission.setTeacherComment(null);
            submission.setIsExcellent(0);
            if (STATUS_IN_PROGRESS.equals(submission.getStatus())) {
                submission.setSubmittedAt(LocalDateTime.now());
            }
            int rows = submissionMapper.update(submission);
            if (rows == 0) {
                throw new BaseException("提交已被他人处理，请刷新后重试");
            }
            examAnswerMapper.deleteBySubmissionId(submission.getSubmissionId());
        }

        for (ExamAnswer answer : answers) {
            answer.setSubmissionId(submission.getSubmissionId());
            examAnswerMapper.insert(answer);
        }
        // 同步成绩记录：全为客观题时自动判分即视为已确认，可直接申诉/汇总；
        // 含填空/简答时留待教师批阅（grade() 会置为已确认），避免学生按未批完的分数申诉
        boolean hasUngraded = answers.stream().anyMatch(a -> a.getIsCorrect() == null);
        upsertGrade(submission, task, !hasUngraded);
        return detail(submission.getSubmissionId());
    }

    // ========== 教师批阅 ==========

    @Override
    public PageResult listByTask(Long taskId, Integer page, Integer pageSize, String status) {
        if (taskId == null) {
            throw new BaseException("任务ID不能为空");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        if (status != null && !status.isBlank() && !STATUS_FILTERS.contains(status)) {
            throw new BaseException("状态筛选仅支持 pending/in_progress/graded");
        }
        // 数据范围收敛：教师/助教只能看自己课程的任务；学生只能看自己的提交
        Long scopedStudentId;
        if (accessGuard.isTeaching()) {
            accessGuard.requireCourseTeacher(task.getCourseId(), "查看任务提交");
            scopedStudentId = null;
        } else if (accessGuard.isStudent()) {
            scopedStudentId = accessGuard.requireUserId("查看任务提交");
        } else {
            throw new ForbiddenException("无权查看任务提交");
        }
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        String st = (status == null || status.isBlank()) ? null : status;

        long total = submissionMapper.countByTaskId(taskId, st, scopedStudentId);
        List<Submission> list = submissionMapper.selectPageByTaskId(taskId, st, scopedStudentId, (p - 1) * ps, ps);
        List<SubmissionVO> records = list.stream()
                .map(s -> SubmissionVO.fromEntity(s, task.getTitle(), task.getMaxScore(), null))
                .toList();
        return PageResult.build(records, total, (long) p, (long) ps);
    }

    @Override
    public SubmissionVO detail(Long submissionId) {
        if (submissionId == null) {
            throw new BaseException("提交ID不能为空");
        }
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        // 越权防护：教师须为该校课程教师；学生只能看自己的提交
        boolean teaching = accessGuard.isTeaching();
        if (teaching) {
            accessGuard.requireCourseTeacher(task.getCourseId(), "查看提交详情");
        } else {
            Long currentUserId = accessGuard.requireUserId("查看提交详情");
            if (!currentUserId.equals(submission.getStudentId())) {
                throw new ForbiddenException("无权查看他人的提交");
            }
        }
        // 参考答案与解析只对教师开放；学生仅在批阅完成后可见
        boolean includeReference = teaching || STATUS_GRADED.equals(submission.getStatus());
        Map<Long, PaperQuestion> pqByQId = paperQuestionMap(task.getTaskId());
        List<ExamAnswer> examAnswers = examAnswerMapper.selectBySubmissionId(submissionId);
        List<ExamAnswerVO> answers = examAnswers.stream()
                .map(a -> buildAnswerVO(a, pqByQId.get(a.getQId()), includeReference))
                .toList();
        return SubmissionVO.fromEntity(submission, task.getTitle(), task.getMaxScore(), answers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmissionVO grade(SubmissionGradeDTO dto) {
        accessGuard.requireTeaching("批阅提交");
        if (dto == null || dto.getSubmissionId() == null) {
            throw new BaseException("提交ID不能为空");
        }
        Submission submission = submissionMapper.selectById(dto.getSubmissionId());
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        accessGuard.requireCourseTeacher(task.getCourseId(), "批阅该任务的提交");
        if (dto.getScore() == null) {
            throw new BaseException("打分不能为空");
        }
        BigDecimal max = task.getMaxScore() == null ? BigDecimal.ZERO : task.getMaxScore();
        if (dto.getScore().compareTo(BigDecimal.ZERO) < 0 || dto.getScore().compareTo(max) > 0) {
            throw new BaseException("分数必须在0~" + max + "之间");
        }
        if (dto.getVersion() == null) {
            throw new BaseException("提交版本号不能为空");
        }

        submission.setStatus(STATUS_GRADED);
        submission.setScore(scale2(dto.getScore()));
        submission.setTeacherComment(dto.getTeacherComment());
        submission.setVersion(dto.getVersion());
        int rows = submissionMapper.update(submission);
        if (rows == 0) {
            throw new BaseException("提交已被他人批阅，请刷新后重试");
        }
        upsertGrade(submission, task);
        return detail(submission.getSubmissionId());
    }

    // ========== 优秀范本 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmissionVO setExcellent(SubmissionExcellentDTO dto) {
        accessGuard.requireTeaching("设置优秀范本");
        if (dto == null || dto.getSubmissionId() == null) {
            throw new BaseException("提交ID不能为空");
        }
        if (dto.getIsExcellent() == null || (dto.getIsExcellent() != 0 && dto.getIsExcellent() != 1)) {
            throw new BaseException("isExcellent 仅支持 1(设为范本)/0(取消)");
        }
        if (dto.getVersion() == null) {
            throw new BaseException("提交版本号不能为空");
        }
        Submission submission = submissionMapper.selectById(dto.getSubmissionId());
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        submission.setIsExcellent(dto.getIsExcellent());
        submission.setVersion(dto.getVersion());
        int rows = submissionMapper.update(submission);
        if (rows == 0) {
            throw new BaseException("提交已被他人处理，请刷新后重试");
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        accessGuard.requireCourseTeacher(task == null ? null : task.getCourseId(), "设置优秀范本");
        return SubmissionVO.fromEntity(submissionMapper.selectById(submission.getSubmissionId()),
                task == null ? null : task.getTitle(),
                task == null ? null : task.getMaxScore(),
                null);
    }

    @Override
    public PageResult listExcellent(Long taskId, Integer page, Integer pageSize) {
        accessGuard.requireTeaching("查看优秀范本");
        if (taskId == null) {
            throw new BaseException("任务ID不能为空");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        accessGuard.requireCourseTeacher(task.getCourseId(), "查看优秀范本");
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        long total = submissionMapper.countExcellentByTaskId(taskId);
        List<Submission> list = submissionMapper.selectExcellentPageByTaskId(taskId, (p - 1) * ps, ps);
        List<SubmissionVO> records = list.stream()
                .map(s -> SubmissionVO.fromEntity(s, task.getTitle(), task.getMaxScore(), null))
                .toList();
        return PageResult.build(records, total, (long) p, (long) ps);
    }

    // ========== 内部方法 ==========

    private List<ExamAnswer> gradeAnswers(List<SubmissionSubmitDTO.AnswerItem> items,
                                          Map<Long, PaperQuestion> pqByQId) {
        List<ExamAnswer> answers = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (SubmissionSubmitDTO.AnswerItem item : items) {
            if (item.getQId() == null) {
                throw new BaseException("题目ID不能为空");
            }
            if (!seen.add(item.getQId())) {
                throw new BaseException("同一题目只能作答一次，题目ID: " + item.getQId());
            }
            PaperQuestion pq = pqByQId.get(item.getQId());
            if (pq == null) {
                throw new BaseException("题目不属于该任务试卷，题目ID: " + item.getQId());
            }
            QuestionBank q = questionBankMapper.selectById(item.getQId());
            if (q == null) {
                throw new BaseException("题目不存在，题目ID: " + item.getQId());
            }

            ExamAnswer answer = new ExamAnswer();
            answer.setQId(item.getQId());
            answer.setStudentAnswer(item.getStudentAnswer());
            answer.setAnsweredAt(LocalDateTime.now());
            gradeObjective(q, item.getStudentAnswer(), pq.getScore(), answer);
            answers.add(answer);
        }
        return answers;
    }

    /**
     * 客观题自动判分；填空/简答主观题不判分（is_correct=null、score=0），留给教师批阅补分。
     */
    private void gradeObjective(QuestionBank q, String studentAnswer, BigDecimal fullScore, ExamAnswer answer) {
        String qType = q.getQType();
        boolean correct = switch (qType == null ? "" : qType) {
            case "single_choice", "multiple_choice" -> choiceMatches(qType, q.getAnswer(), studentAnswer);
            case "judge" -> judgeMatches(q.getAnswer(), studentAnswer);
            default -> false;
        };
        if ("fill_blank".equals(qType) || "short_answer".equals(qType)) {
            answer.setIsCorrect(null);
            answer.setScore(scale2(BigDecimal.ZERO));
            return;
        }
        answer.setIsCorrect(correct ? 1 : 0);
        answer.setScore(correct ? scale2(fullScore) : scale2(BigDecimal.ZERO));
    }

    /**
     * 选择题答案统一转下标后比对；多选按去重排序后的集合比对，选项顺序无关。
     */
    private boolean choiceMatches(String qType, String reference, String student) {
        if (reference == null || student == null) {
            return false;
        }
        Set<String> ref = normalizedChoiceSet(reference);
        Set<String> stu = normalizedChoiceSet(student);
        if (ref.isEmpty() || stu.isEmpty()) {
            return false;
        }
        if ("multiple_choice".equals(qType)) {
            return ref.equals(stu);
        }
        return ref.size() == 1 && stu.size() == 1 && ref.equals(stu);
    }

    private Set<String> normalizedChoiceSet(String answer) {
        Set<String> set = new TreeSet<>();
        for (String p : answer.split("[,，;；、]")) {
            String item = p.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.matches("[A-Da-d]")) {
                set.add(String.valueOf(Character.toUpperCase(item.charAt(0)) - 'A'));
            } else {
                set.add(item);
            }
        }
        return set;
    }

    private boolean judgeMatches(String reference, String student) {
        if (reference == null || student == null) {
            return false;
        }
        return normalizeJudge(reference).equals(normalizeJudge(student));
    }

    private String normalizeJudge(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "t", "正确", "对", "是", "√" -> "1";
            case "0", "false", "f", "错误", "错", "否", "×" -> "0";
            default -> value.trim();
        };
    }

    private BigDecimal totalScore(List<ExamAnswer> answers) {
        return answers.stream()
                .map(a -> a.getScore() == null ? BigDecimal.ZERO : a.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void upsertGrade(Submission submission, Task task) {
        upsertGrade(submission, task, true);
    }

    private void upsertGrade(Submission submission, Task task, boolean confirmed) {
        BigDecimal weight = task.getWeight() == null ? BigDecimal.ZERO : task.getWeight();
        BigDecimal weighted = submission.getScore()
                .multiply(weight)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Grade grade = gradeMapper.selectBySubmissionId(submission.getSubmissionId());
        if (grade == null) {
            grade = new Grade();
            grade.setCourseId(task.getCourseId());
            grade.setStudentId(submission.getStudentId());
            grade.setTaskId(task.getTaskId());
            grade.setSubmissionId(submission.getSubmissionId());
            grade.setScore(submission.getScore());
            grade.setWeightedScore(weighted);
            grade.setComponentType(task.getTaskType());
            grade.setIsConfirmed(confirmed ? 1 : 0);
            gradeMapper.insert(grade);
            return;
        }
        grade.setScore(submission.getScore());
        grade.setWeightedScore(weighted);
        grade.setIsConfirmed(confirmed ? 1 : 0);
        int rows = gradeMapper.update(grade);
        if (rows == 0) {
            throw new BaseException("成绩同步失败，请重试");
        }
    }

    private ExamAnswerVO buildAnswerVO(ExamAnswer answer, PaperQuestion pq, boolean includeReference) {
        ExamAnswerVO vo = new ExamAnswerVO();
        vo.setQId(answer.getQId());
        if (pq != null) {
            vo.setFullScore(pq.getScore());
            vo.setSortOrder(pq.getSortOrder());
        }
        QuestionBank q = questionBankMapper.selectById(answer.getQId());
        if (q != null) {
            vo.setQType(q.getQType());
            vo.setContent(q.getContent());
            vo.setOptions(QuestionValidator.parseJsonList(q.getOptions()));
            // 脱敏：非教师、或学生尚未批阅完成时，不下发参考答案与解析
            if (includeReference) {
                vo.setAnswer(q.getAnswer());
                vo.setAnalysis(q.getAnalysis());
            }
        }
        vo.setStudentAnswer(answer.getStudentAnswer());
        vo.setIsCorrect(answer.getIsCorrect());
        vo.setScore(answer.getScore());
        return vo;
    }

    private Map<Long, PaperQuestion> paperQuestionMap(Long taskId) {
        ExamPaper paper = examPaperMapper.selectByTaskId(taskId);
        if (paper == null) {
            return Collections.emptyMap();
        }
        return paperQuestionMapper.selectByPaperId(paper.getPaperId()).stream()
                .collect(Collectors.toMap(PaperQuestion::getQId, p -> p, (a, b) -> a));
    }

    private Integer isLate(Task task) {
        if (task.getDeadline() == null) {
            return 0;
        }
        return LocalDateTime.now().isAfter(task.getDeadline()) ? 1 : 0;
    }

    private boolean isResubmitAllowed(Task task) {
        return task.getAllowResubmit() != null && task.getAllowResubmit() == 1;
    }

    private boolean isTimedExam(ExamPaper paper) {
        return paper.getDuration() != null && paper.getDuration() > 0;
    }

    private void assertWithinDuration(Submission submission, ExamPaper paper) {
        LocalDateTime start = submission.getSubmittedAt() != null
                ? submission.getSubmittedAt()
                : submission.getCreatedAt();
        long elapsedMinutes = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
        if (elapsedMinutes >= paper.getDuration()) {
            cheatLogService.recordOverTime(submission.getSubmissionId());
            throw new BaseException("作答超时：本卷限时 " + paper.getDuration() + " 分钟，已无法提交");
        }
    }

    private ExamStartVO buildStartVO(Submission submission, ExamPaper paper) {
        LocalDateTime startedAt = submission.getSubmittedAt();
        return ExamStartVO.builder()
                .submissionId(submission.getSubmissionId())
                .taskId(submission.getTaskId())
                .startedAt(startedAt)
                .duration(paper.getDuration())
                .deadline(startedAt.plusMinutes(paper.getDuration()))
                .build();
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 不再用 0 占位：0 不是有效用户，写入会触发外键失败或产生孤儿数据
            throw new ForbiddenException("未登录或登录状态无效");
        }
        return userId;
    }
}
