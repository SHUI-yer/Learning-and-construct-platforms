package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learning.learning.entity.StudyStats;
import com.learning.learning.entity.VideoProgress;
import com.learning.learning.mapper.StudyStatsMapper;
import com.learning.learning.mapper.UserInfoMapper;
import com.learning.learning.mapper.VideoProgressMapper;
import com.learning.learning.service.LearningMonitorService;
import com.learning.learning.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningMonitorServiceImpl implements LearningMonitorService {

    private final StudyStatsMapper studyStatsMapper;
    private final VideoProgressMapper videoProgressMapper;
    private final UserInfoMapper userInfoMapper;

    @Override
    public List<StudentProgressVO> getClassProgress(Long courseId, Long chapterId, int page, int size) {
        Page<StudyStats> p = new Page<>(page, size);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudyStats>();
        if (courseId != null) wrapper.eq(StudyStats::getCourseId, courseId);
        // chapterId is ignored in this simplified implementation
        var result = studyStatsMapper.selectPage(p, wrapper);
        List<StudyStats> records = result.getRecords();

        // Batch fetch user info
        List<Long> userIds = records.stream()
                .map(StudyStats::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Map<String, Object>> userInfoMap = Map.of();
        if (!userIds.isEmpty()) {
            List<Map<String, Object>> userInfoList = userInfoMapper.selectUserInfoBatch(userIds);
            userInfoMap = userInfoList.stream()
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("userId")).longValue(),
                            Function.identity(),
                            (a, b) -> a
                    ));
        }
        final Map<Long, Map<String, Object>> finalUserInfoMap = userInfoMap;

        return records.stream().map(s -> {
            StudentProgressVO vo = new StudentProgressVO();
            vo.setStudentId(s.getUserId());
            Map<String, Object> info = finalUserInfoMap.get(s.getUserId());
            if (info != null) {
                vo.setStudentName(info.get("realName") != null ? String.valueOf(info.get("realName")) : null);
                vo.setAvatarUrl(info.get("avatarUrl") != null ? String.valueOf(info.get("avatarUrl")) : null);
            }
            vo.setVideoPercent(calculatePercent(s.getVideoDuration(), s.getTotalDuration()));
            vo.setDocPercent(calculatePercent(s.getDocDuration(), s.getTotalDuration()));
            // find latest watch time from video_progress
            VideoProgress last = videoProgressMapper.selectLatestByUser(s.getUserId());
            vo.setLastWatchAt(last == null ? null : last.getLastWatchedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private BigDecimal calculatePercent(Integer part, Integer total) {
        if (total == null || total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part == null ? 0 : part).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
    }
}
