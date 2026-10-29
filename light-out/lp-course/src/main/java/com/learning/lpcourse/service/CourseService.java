package com.learning.lpcourse.service;

import com.learning.lpcommon.result.Result;
import com.learning.lpcourse.dto.*;
import com.learning.lpcourse.vo.CourseVO;
import com.learning.lpcommon.result.PageResult;

import java.util.List;

public interface CourseService {
    /**
     * 课程分页查询
     */
    PageResult<CourseVO> getCoursePage(Long pageNum, Long pageSize, CourseQueryDTO query);

    /**
     * 教师自主创建课程（默认草稿0）
     */
    Long createCourse(CourseCreateDTO dto);

    /**
     * 管理员批量导入课程
     * @param itemList 导入课程列表
     */
    void importCourse(List<CourseImportItemDTO> itemList);
    /**
     * 获取课程详情
     */
    CourseVO getCourseInfo(Long courseId);

    /**
     * 管理员审核课程
     */
    Boolean auditCourse(CourseAuditDTO auditDTO);

    /**
     * 复制课程
     */
    Long copyCourse(CourseCopyDTO dto);

    Result<Void> updateCourse(CourseUpdateDTO dto);
}