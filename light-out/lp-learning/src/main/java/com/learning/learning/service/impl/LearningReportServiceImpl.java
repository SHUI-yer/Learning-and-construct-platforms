package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.learning.entity.DocProgress;
import com.learning.learning.entity.LearningReport;
import com.learning.learning.entity.VideoProgress;
import com.learning.learning.mapper.DocProgressMapper;
import com.learning.learning.mapper.LearningReportMapper;
import com.learning.learning.mapper.VideoProgressMapper;
import com.learning.learning.service.LearningReportService;
import com.learning.learning.vo.LearningReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningReportServiceImpl implements LearningReportService {

    private final LearningReportMapper learningReportMapper;
    private final VideoProgressMapper videoProgressMapper;
    private final DocProgressMapper docProgressMapper;
    private final ObjectMapper objectMapper;

    @Override
    public LearningReportVO getOrGenerateReport(Long userId, Long courseId, String type, String period) {
        if (userId == null || courseId == null) {
            return new LearningReportVO();
        }
        // 先查找已有报告，避免重复生成
        LearningReport existing = learningReportMapper.selectByUserCourseTypePeriod(userId, courseId, type, period);
        if (existing != null) {
            LearningReportVO vo = new LearningReportVO();
            vo.setReportId(existing.getReportId());
            vo.setReportType(existing.getReportType());
            vo.setPeriod(existing.getPeriod());
            try {
                vo.setData(objectMapper.readValue(existing.getData(), Map.class));
            } catch (JsonProcessingException e) {
                vo.setData(new LinkedHashMap<>());
            }
            vo.setGeneratedAt(existing.getGeneratedAt());
            return vo;
        }
        // 无已有报告则生成新的
        return generateReport(userId, courseId, type, period);
    }

    @Override
    public LearningReportVO generateReport(Long userId, Long courseId, String type, String period) {
        if (userId == null || courseId == null) {
            return new LearningReportVO();
        }
        List<VideoProgress> videoProgresses = videoProgressMapper.selectList(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId));
        List<DocProgress> docProgresses = docProgressMapper.selectList(new LambdaQueryWrapper<DocProgress>()
                .eq(DocProgress::getUserId, userId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", courseId);
        data.put("reportType", type);
        data.put("period", period);
        data.put("videoProgressCount", videoProgresses.size());
        data.put("docProgressCount", docProgresses.size());
        data.put("videoDuration", videoProgresses.stream().mapToInt(VideoProgress::getDurationWatched).sum());
        data.put("docDuration", docProgresses.stream().mapToInt(DocProgress::getLastPosition).sum());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize learning report failed", e);
        }
        LearningReport entity = new LearningReport();
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setReportType(type);
        entity.setPeriod(period);
        entity.setData(payload);
        entity.setGeneratedAt(LocalDateTime.now());
        learningReportMapper.insert(entity);
        LearningReportVO vo = new LearningReportVO();
        vo.setReportId(entity.getReportId());
        vo.setReportType(type);
        vo.setPeriod(period);
        vo.setData(data);
        vo.setGeneratedAt(entity.getGeneratedAt());
        return vo;
    }

    @Override
    public LearningReportVO getReport(Long reportId) {
        LearningReport entity = learningReportMapper.selectById(reportId);
        if (entity == null) {
            return new LearningReportVO();
        }
        LearningReportVO vo = new LearningReportVO();
        vo.setReportId(entity.getReportId());
        vo.setReportType(entity.getReportType());
        vo.setPeriod(entity.getPeriod());
        try {
            vo.setData(objectMapper.readValue(entity.getData(), Map.class));
        } catch (JsonProcessingException e) {
            vo.setData(new LinkedHashMap<>());
        }
        vo.setGeneratedAt(entity.getGeneratedAt());
        return vo;
    }
}
