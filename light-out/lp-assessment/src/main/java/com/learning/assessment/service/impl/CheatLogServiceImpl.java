package com.learning.assessment.service.impl;

import com.learning.assessment.dto.CheatLogRecordDTO;
import com.learning.assessment.entity.CheatLog;
import com.learning.assessment.entity.Submission;
import com.learning.assessment.entity.Task;
import com.learning.assessment.mapper.CheatLogMapper;
import com.learning.assessment.mapper.SubmissionMapper;
import com.learning.assessment.mapper.TaskMapper;
import com.learning.assessment.service.CheatLogService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.CheatLogVO;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.exception.ForbiddenException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CheatLogServiceImpl implements CheatLogService {

    private static final String CHEAT_TYPE_OVER_TIME = "over_time";
    private static final Set<String> CHEAT_TYPES = Set.of("tab_switch", "copy_paste", "right_click", "over_time");
    private static final Map<String, String> CHEAT_TYPE_NAMES = Map.of(
            "tab_switch", "切屏切换",
            "copy_paste", "复制粘贴",
            "right_click", "右键异常",
            CHEAT_TYPE_OVER_TIME, "作答超时");

    @Resource
    private CheatLogMapper cheatLogMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private AssessmentAccessGuard accessGuard;

    // ========== 记录异常行为 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CheatLogVO> record(CheatLogRecordDTO dto) {
        accessGuard.requireStudent("上报异常行为");
        if (dto == null || dto.getSubmissionId() == null) {
            throw new BaseException("提交ID不能为空");
        }
        if (dto.getEvents() == null || dto.getEvents().isEmpty()) {
            throw new BaseException("异常行为记录不能为空");
        }
        Submission submission = submissionMapper.selectById(dto.getSubmissionId());
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        // 只能为自己的作答上报：避免伪造他人切屏记录（栽赃）或刷爆接口
        Long currentUserId = accessGuard.requireUserId("上报异常行为");
        if (!currentUserId.equals(submission.getStudentId())) {
            throw new ForbiddenException("只能上报自己的作答记录");
        }
        List<CheatLogVO> result = new ArrayList<>();
        for (CheatLogRecordDTO.CheatEvent event : dto.getEvents()) {
            if (event == null || event.getCheatType() == null || event.getCheatType().isBlank()) {
                throw new BaseException("异常类型不能为空");
            }
            String cheatType = event.getCheatType().trim();
            if (!CHEAT_TYPES.contains(cheatType)) {
                throw new BaseException("不支持的异常类型：" + cheatType
                        + "（仅支持 tab_switch/copy_paste/right_click/over_time）");
            }
            CheatLog log = new CheatLog();
            log.setSubmissionId(dto.getSubmissionId());
            log.setCheatType(cheatType);
            log.setDetail(event.getDetail());
            log.setOccurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now());
            cheatLogMapper.insert(log);
            result.add(toVO(log));
        }
        return result;
    }

    // ========== 异常记录查询 ==========

    @Override
    public List<CheatLogVO> listBySubmission(Long submissionId) {
        if (submissionId == null) {
            throw new BaseException("提交ID不能为空");
        }
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        requireSubmissionTeacher(submission, "查看异常行为记录");
        return cheatLogMapper.selectBySubmissionId(submissionId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public int countBySubmission(Long submissionId, String cheatType) {
        if (submissionId == null) {
            throw new BaseException("提交ID不能为空");
        }
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BaseException("提交不存在");
        }
        requireSubmissionTeacher(submission, "查看异常行为统计");
        if (cheatType == null || cheatType.isBlank()) {
            return cheatLogMapper.countBySubmissionId(submissionId);
        }
        String type = cheatType.trim();
        if (!CHEAT_TYPES.contains(type)) {
            throw new BaseException("不支持的异常类型：" + type
                    + "（仅支持 tab_switch/copy_paste/right_click/over_time）");
        }
        return cheatLogMapper.countBySubmissionIdAndType(submissionId, type);
    }

    // ========== 服务端超时留痕（独立事务，避免随拒交回滚丢失） ==========

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOverTime(Long submissionId) {
        List<CheatLog> exists = cheatLogMapper.selectBySubmissionIdAndType(submissionId, CHEAT_TYPE_OVER_TIME);
        if (!exists.isEmpty()) {
            return;
        }
        CheatLog log = new CheatLog();
        log.setSubmissionId(submissionId);
        log.setCheatType(CHEAT_TYPE_OVER_TIME);
        log.setDetail("作答超时：提交时间超过试卷限时");
        log.setOccurredAt(LocalDateTime.now());
        cheatLogMapper.insert(log);
    }

    /** 教师端操作：当前用户必须能管理该提交所属课程 */
    private void requireSubmissionTeacher(Submission submission, String action) {
        accessGuard.requireTeaching(action);
        Task task = taskMapper.selectById(submission.getTaskId());
        if (task == null) {
            throw new BaseException("任务不存在");
        }
        accessGuard.requireCourseTeacher(task.getCourseId(), action);
    }

    private CheatLogVO toVO(CheatLog log) {        return CheatLogVO.builder()
                .id(log.getId())
                .submissionId(log.getSubmissionId())
                .cheatType(log.getCheatType())
                .cheatTypeName(CHEAT_TYPE_NAMES.getOrDefault(log.getCheatType(), log.getCheatType()))
                .detail(log.getDetail())
                .occurredAt(log.getOccurredAt())
                .build();
    }
}
