package com.learning.assessment.service.impl;

import com.learning.assessment.dto.GradeDesignDTO;
import com.learning.assessment.dto.TaskCreateDTO;
import com.learning.assessment.entity.GradeScheme;
import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.GradeSchemeMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.service.TaskService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.GradeDesignVO;
import com.learning.assessment.vo.TaskVO;
import com.learning.lpcommon.exception.BaseException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Resource
    private GradeSchemeMapper gradeSchemeMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private AssessmentAccessGuard accessGuard;

    private static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100.00");
    private static final String PUBLISH_IMMEDIATE = "immediate";
    private static final String PUBLISH_SCHEDULED = "scheduled";

    // ========== 成绩构成 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<GradeDesignVO> saveGradeDesign(GradeDesignDTO dto) {
        accessGuard.requireTeaching("配置成绩构成");
        Long courseId = dto.getCourseId();
        accessGuard.requireCourseTeacher(courseId, "配置成绩构成");
        List<GradeDesignDTO.SchemeItem> items = dto.getItems();

        BigDecimal sum = items.stream()
                .map(GradeDesignDTO.SchemeItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(TOTAL_WEIGHT) != 0) {
            throw new BaseException("成绩构成权重总和必须为 100%，当前总和：" + sum + "%");
        }

        gradeSchemeMapper.deleteByCourseId(courseId);

        for (int i = 0; i < items.size(); i++) {
            GradeDesignDTO.SchemeItem item = items.get(i);
            GradeScheme scheme = new GradeScheme();
            scheme.setCourseId(courseId);
            scheme.setComponentType(item.getComponentType());
            scheme.setWeight(item.getWeight());
            scheme.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i + 1);
            gradeSchemeMapper.insert(scheme);
        }

        List<GradeScheme> savedList = gradeSchemeMapper.selectByCourseId(courseId);
        return GradeDesignVO.fromEntityList(savedList);
    }

    @Override
    public List<GradeDesignVO> getGradeDesign(Long courseId) {
        accessGuard.requireTeaching("查看成绩构成");
        accessGuard.requireCourseTeacher(courseId, "查看成绩构成");
        List<GradeScheme> list = gradeSchemeMapper.selectByCourseId(courseId);
        return GradeDesignVO.fromEntityList(list);
    }

    // ========== 任务管理 ==========

    private Task buildTask(TaskCreateDTO dto) {
        return Task.builder()
                .taskId(dto.getTaskId())
                .courseId(dto.getCourseId())
                .chapterId(dto.getChapterId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .taskType(dto.getTaskType())
                .tag(dto.getTag())
                .weight(dto.getWeight())
                .publishType(dto.getPublishType())
                .publishTime(dto.getPublishTime())
                .deadline(dto.getDeadline())
                .allowLate(dto.getAllowLate())
                .latePenalty(dto.getLatePenalty())
                .maxScore(dto.getMaxScore())
                .allowResubmit(dto.getAllowResubmit())
                .attachmentUrls(dto.getAttachmentUrls())
                .build();
    }

    @Override
    public TaskVO createTask(TaskCreateDTO dto) {
        accessGuard.requireTeaching("创建任务");
        accessGuard.requireCourseTeacher(dto == null ? null : dto.getCourseId(), "创建任务");
        validatePublishStrategy(dto);
        Task task = buildTask(dto);
        taskMapper.insert(task);
        return TaskVO.fromEntity(task);
    }

    @Override
    public TaskVO updateTask(TaskCreateDTO dto) {
        if (dto.getTaskId() == null) {
            throw new BaseException("更新任务时 taskId 不能为空");
        }

        Task existing = taskMapper.selectById(dto.getTaskId());
        if (existing == null) {
            throw new BaseException("任务不存在或已删除");
        }
        // 以库里任务的课程为准做归属校验，避免调用方伪造 courseId
        accessGuard.requireTeaching("修改任务");
        accessGuard.requireCourseTeacher(existing.getCourseId(), "修改任务");

        validatePublishStrategy(dto);

        Task task = buildTask(dto);
        task.setVersion(existing.getVersion());

        int rows = taskMapper.update(task);
        if (rows == 0) {
            throw new BaseException("任务已被他人修改，请刷新后重试");
        }

        Task updated = taskMapper.selectById(dto.getTaskId());
        return TaskVO.fromEntity(updated);
    }

    // ========== 发布策略 ==========

    private void validatePublishStrategy(TaskCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        String publishType = dto.getPublishType();
        LocalDateTime publishTime = dto.getPublishTime();
        LocalDateTime deadline = dto.getDeadline();
        Integer allowLate = dto.getAllowLate();
        BigDecimal latePenalty = dto.getLatePenalty();

        if (publishType == null || publishType.isEmpty()) {
            throw new BaseException("发布方式不能为空（immediate-立即 / scheduled-定时）");
        }
        if (!PUBLISH_IMMEDIATE.equals(publishType) && !PUBLISH_SCHEDULED.equals(publishType)) {
            throw new BaseException("发布方式不合法，仅支持 immediate-立即 / scheduled-定时");
        }

        if (PUBLISH_SCHEDULED.equals(publishType)) {
            if (publishTime == null) {
                throw new BaseException("定时发布必须设置发布时间");
            }
            if (!publishTime.isAfter(now)) {
                throw new BaseException("定时发布时间必须晚于当前时间");
            }
        } else {
            dto.setPublishTime(null);
        }

        if (deadline == null) {
            throw new BaseException("截止时间不能为空");
        }
        LocalDateTime baseTime = PUBLISH_SCHEDULED.equals(publishType) ? publishTime : now;
        if (!deadline.isAfter(baseTime)) {
            throw new BaseException("截止时间必须晚于" + (PUBLISH_SCHEDULED.equals(publishType) ? "发布时间" : "当前时间"));
        }

        if (allowLate != null && allowLate == 1) {
            if (latePenalty == null) {
                throw new BaseException("允许迟交时必须设置扣分比例");
            }
            if (latePenalty.compareTo(BigDecimal.ZERO) <= 0 || latePenalty.compareTo(new BigDecimal("100.00")) > 0) {
                throw new BaseException("迟交扣分比例必须在(0, 100]范围内");
            }
        } else {
            // 不允许迟交：扣分比例无意义，强制清空并统一为0
            dto.setAllowLate(0);
            dto.setLatePenalty(null);
        }
    }

    @Override
    public void deleteTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("任务不存在或已删除");
        }
        accessGuard.requireTeaching("删除任务");
        accessGuard.requireCourseTeacher(task.getCourseId(), "删除任务");
        taskMapper.softDelete(taskId);
    }

    @Override
    public TaskVO getTaskById(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("任务不存在或已删除");
        }
        return TaskVO.fromEntity(task);
    }

    @Override
    public List<TaskVO> listTasksByCourse(Long courseId) {
        List<Task> list = taskMapper.selectByCourseId(courseId);
        return TaskVO.fromEntityList(list);
    }

    @Override
    public List<TaskVO> listTasksByConditions(Long courseId, String taskType, String keyword) {
        List<Task> list = taskMapper.selectByConditions(courseId, taskType, keyword);
        return TaskVO.fromEntityList(list);
    }
}