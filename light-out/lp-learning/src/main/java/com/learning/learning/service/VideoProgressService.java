package com.learning.learning.service;

import com.learning.learning.dto.VideoProgressDTO;
import com.learning.learning.vo.VideoProgressVO;

public interface VideoProgressService {

    void reportProgress(VideoProgressDTO dto, Long userId);

    VideoProgressVO getProgress(Long resourceId, Long userId);

    void batchFlushToDB();

    void syncProgressFromReport(java.util.Map<String, Object> payload);
}
