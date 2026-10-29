package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learning.learning.entity.StudyStats;
import com.learning.learning.mapper.StudyStatsMapper;
import com.learning.learning.service.StudyStatsService;
import com.learning.learning.vo.CourseProgressVO;
import com.learning.learning.vo.CourseRankingVO;
import com.learning.learning.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyStatsServiceImpl implements StudyStatsService {

    private final StudyStatsMapper studyStatsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${learning.redis.key-prefix:lp}")
    private String keyPrefix;

    @Override
    public void addDuration(Long userId, Long courseId, int seconds, String type) {
        if (userId == null || courseId == null) {
            return;
        }
        String date = LocalDate.now().toString();
        String key = buildStatsKey(userId, courseId, date);
        Map<Object, Object> current = redisTemplate.opsForHash().entries(key);
        int videoDuration = asInt(current.get("videoDuration"));
        int docDuration = asInt(current.get("docDuration"));
        int totalDuration = asInt(current.get("totalDuration"));
        if ("doc".equalsIgnoreCase(type)) {
            docDuration += seconds;
        } else {
            videoDuration += seconds;
        }
        totalDuration = videoDuration + docDuration;
        Map<String, Object> values = new HashMap<>();
        values.put("videoDuration", videoDuration);
        values.put("docDuration", docDuration);
        values.put("totalDuration", totalDuration);
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    @Override
    public CourseProgressVO getStats(Long userId, Long courseId, LocalDate date) {
        if (userId == null || courseId == null || date == null) {
            return new CourseProgressVO();
        }
        String key = buildStatsKey(userId, courseId, date.toString());
        Map<Object, Object> current = redisTemplate.opsForHash().entries(key);
        if (!current.isEmpty()) {
            return toVo(current);
        }
        StudyStats entity = studyStatsMapper.selectOne(new LambdaQueryWrapper<StudyStats>()
                .eq(StudyStats::getUserId, userId)
                .eq(StudyStats::getCourseId, courseId)
                .eq(StudyStats::getStatDate, date));
        if (entity == null) {
            return new CourseProgressVO();
        }
        CourseProgressVO vo = new CourseProgressVO();
        vo.setVideoPercent(calculatePercent(entity.getVideoDuration(), entity.getTotalDuration()));
        vo.setDocPercent(calculatePercent(entity.getDocDuration(), entity.getTotalDuration()));
        vo.setTotalDuration(entity.getTotalDuration());
        vo.setTasksCompleted(Math.max(0, entity.getTotalDuration() / 60));
        return vo;
    }

    @Override
    public void dailyAggregation() {
        Set<String> keys = redisTemplate.keys(String.format("%s:ss:*", keyPrefix));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
            if (values.isEmpty()) {
                continue;
            }
            String[] parts = key.split(":");
            Long userId = parseLong(parts[2]);
            Long courseId = parseLong(parts[3]);
            String dateValue = parts[4];
            LocalDate statDate = LocalDate.parse(dateValue);
            StudyStats entity = studyStatsMapper.selectOne(new LambdaQueryWrapper<StudyStats>()
                    .eq(StudyStats::getUserId, userId)
                    .eq(StudyStats::getCourseId, courseId)
                    .eq(StudyStats::getStatDate, statDate));
            if (entity == null) {
                entity = new StudyStats();
                entity.setUserId(userId);
                entity.setCourseId(courseId);
                entity.setStatDate(statDate);
            }
            entity.setVideoDuration(asInt(values.get("videoDuration")));
            entity.setDocDuration(asInt(values.get("docDuration")));
            entity.setTotalDuration(asInt(values.get("totalDuration")));
            if (entity.getId() == null) {
                studyStatsMapper.insert(entity);
            } else {
                studyStatsMapper.updateById(entity);
            }
            redisTemplate.delete(key);
        }
    }

    @Override
    public DashboardVO getDashboard(Long courseId) {
        DashboardVO vo = new DashboardVO();
        String today = LocalDate.now().toString();
        Long dau = studyStatsMapper.countDistinctUsersByDate(today);
        Long weeklyActive = studyStatsMapper.countDistinctUsersSince(LocalDate.now().minusDays(7).toString());
        vo.setDau(dau == null ? 0L : dau);
        vo.setWeeklyActive(weeklyActive == null ? 0L : weeklyActive);
        java.math.BigDecimal avg = java.math.BigDecimal.ZERO;
        if (courseId != null) {
            var res = studyStatsMapper.avgCompletionForCourse(courseId);
            if (res != null) avg = res;
        }
        vo.setAvgCompletion(avg == null ? java.math.BigDecimal.ZERO : avg);
        vo.setTopChapters(java.util.List.of("Chapter 1", "Chapter 2"));

        // 课程访问量排行榜（近7天）
        java.util.List<java.util.Map<String, Object>> rankingRaw = studyStatsMapper.courseVisitRanking(
                LocalDate.now().minusDays(7).toString(), 10);
        java.util.List<CourseRankingVO> ranking = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : rankingRaw) {
            CourseRankingVO item = new CourseRankingVO();
            Object cid = row.get("courseId");
            Object vc = row.get("visitCount");
            item.setCourseId(cid instanceof Number ? ((Number) cid).longValue() : null);
            item.setCourseName("Course " + (cid == null ? "?" : cid.toString()));
            item.setVisitCount(vc instanceof Number ? ((Number) vc).longValue() : 0L);
            ranking.add(item);
        }
        vo.setCourseRanking(ranking);

        // 作业提交完成率
        java.math.BigDecimal submissionRate = java.math.BigDecimal.ZERO;
        if (courseId != null) {
            var rate = studyStatsMapper.assignmentSubmissionRate(courseId);
            if (rate != null) submissionRate = rate;
        }
        vo.setAssignmentSubmissionRate(submissionRate);

        return vo;
    }

    private String buildStatsKey(Long userId, Long courseId, String date) {
        return String.format("%s:ss:%d:%d:%s", keyPrefix, userId, courseId, date);
    }

    private CourseProgressVO toVo(Map<Object, Object> current) {
        CourseProgressVO vo = new CourseProgressVO();
        int videoDuration = asInt(current.get("videoDuration"));
        int docDuration = asInt(current.get("docDuration"));
        int total = asInt(current.get("totalDuration"));
        vo.setVideoPercent(calculatePercent(videoDuration, total));
        vo.setDocPercent(calculatePercent(docDuration, total));
        vo.setTotalDuration(total);
        vo.setTasksCompleted(Math.max(0, total / 60));
        return vo;
    }

    private java.math.BigDecimal calculatePercent(int part, int total) {
        if (total <= 0) {
            return java.math.BigDecimal.ZERO;
        }
        return java.math.BigDecimal.valueOf(part)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
