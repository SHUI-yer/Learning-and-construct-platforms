package com.learning.learning.consumer;

import com.learning.learning.service.LearningReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportGenerateConsumer {

    private final LearningReportService learningReportService;

    @RabbitListener(queues = "learning.report.generate")
    public void handle(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        Long userId = payload.containsKey("userId") ? ((Number) payload.get("userId")).longValue() : null;
        Long courseId = payload.containsKey("courseId") ? ((Number) payload.get("courseId")).longValue() : null;
        String type = payload.containsKey("type") ? String.valueOf(payload.get("type")) : "weekly";
        String period = payload.containsKey("period") ? String.valueOf(payload.get("period")) : "week";
        if (userId != null && courseId != null) {
            learningReportService.generateReport(userId, courseId, type, period);
        }
    }
}
