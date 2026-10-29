package com.learning.assessment.service;

import com.learning.assessment.dto.GradeAppealCreateDTO;
import com.learning.assessment.dto.GradeAppealReviewDTO;
import com.learning.assessment.vo.GradeAppealVO;
import com.learning.lpcommon.result.PageResult;

public interface GradeAppealService {

    GradeAppealVO createAppeal(GradeAppealCreateDTO dto);

    PageResult<GradeAppealVO> listMyAppeals(Integer page, Integer pageSize);

    // 按提交ID查成绩记录ID（无记录返回 null），供学生申诉入口使用
    Long getGradeIdBySubmission(Long submissionId);

    PageResult<GradeAppealVO> listByCourse(Long courseId, String status, Integer page, Integer pageSize);

    GradeAppealVO detail(Long appealId);

    GradeAppealVO review(GradeAppealReviewDTO dto);
}
