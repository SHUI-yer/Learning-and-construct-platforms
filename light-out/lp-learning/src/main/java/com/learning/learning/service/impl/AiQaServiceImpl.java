package com.learning.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.learning.dto.AiQaDTO;
import com.learning.learning.entity.AiQaLog;
import com.learning.learning.mapper.AiQaLogMapper;
import com.learning.learning.service.AiQaService;
import com.learning.learning.vo.AiQaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQaServiceImpl implements AiQaService {

    private final AiQaLogMapper aiQaLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiQaVO ask(Long userId, AiQaDTO dto) {
        if (userId == null || dto == null || dto.getResourceId() == null || dto.getQuestion() == null) {
            return new AiQaVO();
        }
        AiQaVO vo = new AiQaVO();
        vo.setQuestion(dto.getQuestion());
        vo.setAnswer("This is a mock AI answer: based on the current resource content, please continue watching and summarize the key knowledge points.");
        vo.setSourceRefs(List.of("chapter:1", "timestamp:00:30"));
        LocalDateTime now = LocalDateTime.now();
        vo.setAskedAt(now);

        AiQaLog entity = new AiQaLog();
        entity.setUserId(userId);
        entity.setResourceId(dto.getResourceId());
        entity.setQuestion(dto.getQuestion());
        entity.setAnswer(vo.getAnswer());
        try {
            entity.setSourceRefs(objectMapper.writeValueAsString(vo.getSourceRefs()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize ai qa refs failed", e);
        }
        entity.setAskedAt(LocalDateTime.now());
        aiQaLogMapper.insert(entity);
        return vo;
    }

    @Override
    public List<AiQaVO> getHistory(Long userId, Long resourceId) {
        LambdaQueryWrapper<AiQaLog> wrapper = new LambdaQueryWrapper<AiQaLog>()
                .eq(AiQaLog::getUserId, userId);
        if (resourceId != null) {
            wrapper.eq(AiQaLog::getResourceId, resourceId);
        }
        wrapper.orderByDesc(AiQaLog::getAskedAt);
        List<AiQaLog> logs = aiQaLogMapper.selectList(wrapper);
        return logs.stream().map(log -> {
            AiQaVO vo = new AiQaVO();
            vo.setQuestion(log.getQuestion());
            vo.setAnswer(log.getAnswer());
            vo.setAskedAt(log.getAskedAt());
            try {
                vo.setSourceRefs(objectMapper.readValue(log.getSourceRefs(), new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                vo.setSourceRefs(new ArrayList<>());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
