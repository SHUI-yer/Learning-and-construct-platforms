package com.learning.lpcourse.mapper;

import com.learning.lpcourse.dto.CourseCopyDTO;
import com.learning.lpcourse.dto.CourseQueryDTO;
import com.learning.lpcourse.entity.Course;
import com.learning.lpcourse.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CourseMapper {

    /**
     * 新增课程
     */
    void insert(Course course);

    /**
     * 分页查询
     */
    List<CourseVO> selectCoursePage(@Param("offset") Long offset,
                                    @Param("pageSize") Long pageSize,
                                    @Param("query") CourseQueryDTO query);

    /**
     * 统计总数
     */
    Long selectCourseCount(@Param("query") CourseQueryDTO query);

    /**
     * 根据id查询课程详情（过滤已删除）
     */
    CourseVO selectCourseById(@Param("courseId") Long courseId);

    /**
     * 更新课程状态（审核使用）
     */
    int updateCourseStatus(Course course);

    /**
     * 查询源课程信息（复制课程使用）
     */
    CourseVO selectSourceCourse(@Param("sourceCourseId") Long sourceCourseId);

    /**
     * 更新课程（乐观锁）
     */
    int update(Course course);

}