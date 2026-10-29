package com.learning.learning.consumer;

import com.learning.learning.service.StudyStatsService;
import com.learning.learning.service.VideoProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayReportConsumer {

    private final com.learning.learning.service.StudyStatsService studyStatsService;
    private final com.learning.learning.service.VideoProgressService videoProgressService;

    @RabbitListener(queues = "learning.play.report")
    public void handle(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        try {
            Long userId = payload.containsKey("userId") ? ((Number) payload.get("userId")).longValue() : null;
            Long courseId = extractLong(payload, "courseId", 1L);
            Long resourceId = payload.containsKey("resourceId") ? ((Number) payload.get("resourceId")).longValue() : null;
            int durationSeconds = extractInt(payload, "durationSeconds", 30);
            String resourceType = payload.containsKey("resourceType") ? String.valueOf(payload.get("resourceType")) : "video";
            if (userId != null && resourceId != null) {
                studyStatsService.addDuration(userId, courseId, durationSeconds, resourceType);
                videoProgressService.syncProgressFromReport(payload);
            }
        } catch (Exception ex) {
            log.error("Failed to process play report message: {}", payload, ex);
            // rethrow to let RabbitMQ retry according to broker settings
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    private Long extractLong(Map<String, Object> payload, String key, Long defaultValue) {
        if (payload.containsKey(key) && payload.get(key) instanceof Number num) {
            return num.longValue();
        }
        return defaultValue;
    }

    private int extractInt(Map<String, Object> payload, String key, int defaultValue) {
        if (payload.containsKey(key) && payload.get(key) instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }
}
