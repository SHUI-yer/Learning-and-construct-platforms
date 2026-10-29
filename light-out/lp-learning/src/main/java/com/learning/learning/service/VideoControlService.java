package com.learning.learning.service;

import com.learning.learning.dto.VideoControlDTO;
import com.learning.learning.vo.VideoControlVO;

public interface VideoControlService {

    void setControl(VideoControlDTO dto);

    VideoControlVO getControl(Long resourceId);
}
