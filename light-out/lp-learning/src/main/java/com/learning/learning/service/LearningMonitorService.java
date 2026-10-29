package com.learning.learning.service;

import com.learning.learning.vo.StudentProgressVO;

import java.util.List;

public interface LearningMonitorService {
    List<StudentProgressVO> getClassProgress(Long courseId, Long chapterId, int page, int size);
}
