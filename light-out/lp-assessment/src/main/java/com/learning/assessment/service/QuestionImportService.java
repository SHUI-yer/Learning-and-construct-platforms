package com.learning.assessment.service;

import com.learning.assessment.vo.QuestionImportVO;
import com.learning.lpcommon.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionImportService {

    QuestionImportVO importQuestions(MultipartFile file, Long courseId);

    PageResult listImports(Long courseId, Integer page, Integer pageSize);

    QuestionImportVO getImportDetail(Long id);
}
