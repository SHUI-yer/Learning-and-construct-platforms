package com.learning.learning.scheduler;

import com.learning.learning.service.DocProgressService;
import com.learning.learning.service.StudyStatsService;
import com.learning.learning.service.VideoProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyStatsScheduler {

    private final VideoProgressService videoProgressService;
    private final DocProgressService docProgressService;
    private final StudyStatsService studyStatsService;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void flushProgressToDb() {
        videoProgressService.batchFlushToDB();
        docProgressService.batchFlushToDB();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void aggregateDailyStats() {
        studyStatsService.dailyAggregation();
    }
}
