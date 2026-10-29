package com.learning.learning.service;

import com.learning.learning.dto.SubtitleDTO;
import com.learning.learning.dto.VideoQualityDTO;

import java.util.List;

public interface VideoEnhancementService {
    void addSubtitle(SubtitleDTO dto);
    List<SubtitleDTO> listSubtitles(Long resourceId);

    void addQuality(VideoQualityDTO dto);
    List<VideoQualityDTO> listQualities(Long resourceId);
}
