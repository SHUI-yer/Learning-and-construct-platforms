package com.learning.learning.service.impl;

import com.learning.learning.dto.SubtitleDTO;
import com.learning.learning.dto.VideoQualityDTO;
import com.learning.learning.entity.Subtitle;
import com.learning.learning.entity.VideoQuality;
import com.learning.learning.mapper.SubtitleMapper;
import com.learning.learning.mapper.VideoQualityMapper;
import com.learning.learning.service.VideoEnhancementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoEnhancementServiceImpl implements VideoEnhancementService {

    private final SubtitleMapper subtitleMapper;
    private final VideoQualityMapper videoQualityMapper;

    @Override
    public void addSubtitle(SubtitleDTO dto) {
        Subtitle s = new Subtitle();
        s.setResourceId(dto.getResourceId());
        s.setLanguage(dto.getLanguage());
        s.setUrl(dto.getUrl());
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        subtitleMapper.insert(s);
    }

    @Override
    public List<SubtitleDTO> listSubtitles(Long resourceId) {
        List<Subtitle> list = subtitleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Subtitle>()
                .eq(Subtitle::getResourceId, resourceId));
        return list.stream().map(it -> {
            SubtitleDTO d = new SubtitleDTO();
            d.setResourceId(it.getResourceId());
            d.setLanguage(it.getLanguage());
            d.setUrl(it.getUrl());
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public void addQuality(VideoQualityDTO dto) {
        VideoQuality q = new VideoQuality();
        q.setResourceId(dto.getResourceId());
        q.setLabel(dto.getLabel());
        q.setResolution(dto.getResolution());
        q.setUrl(dto.getUrl());
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        videoQualityMapper.insert(q);
    }

    @Override
    public List<VideoQualityDTO> listQualities(Long resourceId) {
        List<VideoQuality> list = videoQualityMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VideoQuality>()
                .eq(VideoQuality::getResourceId, resourceId));
        return list.stream().map(it -> {
            VideoQualityDTO d = new VideoQualityDTO();
            d.setResourceId(it.getResourceId());
            d.setLabel(it.getLabel());
            d.setResolution(it.getResolution());
            d.setUrl(it.getUrl());
            return d;
        }).collect(Collectors.toList());
    }
}
