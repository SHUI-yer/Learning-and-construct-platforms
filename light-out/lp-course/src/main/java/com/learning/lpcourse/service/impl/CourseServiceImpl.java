package com.learning.lpcourse.service.impl;

import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.exception.NotFoundCourseException;
import com.learning.lpcommon.exception.NullException;
import com.learning.lpcommon.result.Result;
import com.learning.lpcourse.dto.*;
import com.learning.lpcourse.entity.*;
import com.learning.lpcourse.mapper.*;
import com.learning.lpcourse.service.CourseService;
import com.learning.lpcourse.vo.CourseVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseAuditMapper courseAuditMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentClassMapper studentClassMapper;
    private final ChapterMapper chapterMapper;
    private final ContentResourceMapper resourceMapper;
    /**
     * 根据课程id查询全部章节
     */

    @Override
    public PageResult<CourseVO> getCoursePage(Long pageNum, Long pageSize, CourseQueryDTO query) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        long offset = (pageNum - 1) * pageSize;
        List<CourseVO> records = courseMapper.selectCoursePage(offset, pageSize, query);
        Long total = courseMapper.selectCourseCount(query);
        return PageResult.build(records, total, pageNum, pageSize);
    }

    @Override
    public Long createCourse(CourseCreateDTO dto) {
        // 参数校验封装
        if (dto == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (dto.getCourseName() == null || dto.getCourseName().isBlank()) {
            throw new IllegalArgumentException("课程名称 courseName 不能为空");
        }
        if (dto.getSemester() == null) {
            throw new IllegalArgumentException("学期 semester 不能为空");
        }
        if (dto.getIsPublic() == null) {
            throw new IllegalArgumentException("是否公开课 isPublic 不能为空");
        }

        //仅任课教师可调用该自建接口
        String roleCode = UserContext.getRoleCode();
        if (!RoleConstant.TEACHER.equals(roleCode)) {
            throw new ForbiddenException("接口仅限教师自主创建课程，管理员请使用导入接口");
        }

        Long loginUserId = UserContext.getUserId();


        if (loginUserId == null) {
            throw new ForbiddenException("用户未登录，请重新登录！");
        }


        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        course.setTeacherId(loginUserId);
        course.setStatus(0);
        course.setVersion(0);
        courseMapper.insert(course);
        log.info("教师{}新建课程，课程ID：{}", loginUserId, course.getCourseId());
        return course.getCourseId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importCourse(List<CourseImportItemDTO> itemList) {
        if (itemList == null || itemList.isEmpty()) {
            throw new NullException("导入课程数据不能为空");
        }
        //仅超级管理员、分院管理员允许导入
        String roleCode = UserContext.getRoleCode();
        if (!RoleConstant.SUPER_ADMIN.equals(roleCode) && !RoleConstant.DEPT_ADMIN.equals(roleCode)) {
            throw new ForbiddenException("仅管理员拥有课程导入权限");
        }

        for (CourseImportItemDTO item : itemList) {
            if (item == null) {
                throw new NullException("课程导入条目不能为空");
            }
            if (item.getCourseName() == null) {
                throw new NullException("课程名称 courseName 不能为空");
            }
            if (item.getSemester() == null) {
                throw new NullException("学期 semester 不能为空");
            }
            if (item.getIsPublic() == null) {
                throw new NullException("是否公开课 isPublic 不能为空");
            }
            if (item.getTeacherId() == null) {
                throw new NullException("课程【" + item.getCourseName() + "】授课教师 teacherId 不能为空");
            }

            //1.新增课程
            Course course = new Course();
            BeanUtils.copyProperties(item, course);
            course.setStatus(0);
            course.setVersion(0);
            courseMapper.insert(course);
            Long newCourseId = course.getCourseId();

            //2.插入主讲教师到course_teacher
            CourseTeacher courseTeacher = new CourseTeacher();
            courseTeacher.setCourseId(newCourseId);
            courseTeacher.setTeacherId(item.getTeacherId());
            courseTeacher.setRole("lecturer");
            courseTeacherMapper.insert(courseTeacher);

            //3.收集所有学生ID，Set自动去重
            Set<Long> allStudentIds = new HashSet<>();

            //方式1：根据班级获取学生
            List<Long> classIdList = item.getClassIdList();
            if (classIdList != null && !classIdList.isEmpty()) {
                List<Long> studentIdsByClass = studentClassMapper.selectStudentIdByClassIds(classIdList);
                allStudentIds.addAll(studentIdsByClass);
            }
            //方式2：直接指定学生
            List<Long> directStudentList = item.getStudentIdList();
            if (directStudentList != null && !directStudentList.isEmpty()) {
                allStudentIds.addAll(directStudentList);
            }

            //4.过滤：移除已经存在本课程的学生，避免唯一索引报错
            if (!allStudentIds.isEmpty()) {
                List<Long> existStudentIds = courseStudentMapper.selectStudentIdByCourseId(newCourseId);
                allStudentIds.removeAll(existStudentIds);

                if (!allStudentIds.isEmpty()) {
                    List<CourseStudent> studentRelationList = new ArrayList<>();
                    for (Long sid : allStudentIds) {
                        CourseStudent cs = new CourseStudent();
                        cs.setCourseId(newCourseId);
                        cs.setStudentId(sid);
                        cs.setJoinType("required");
                        studentRelationList.add(cs);
                    }
                    courseStudentMapper.insertBatch(studentRelationList);
                }
            }
        }
    }

    @Override
    public CourseVO getCourseInfo(Long courseId) {
        return courseMapper.selectCourseById(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditCourse(CourseAuditDTO auditDTO) {
        Long loginUserId = UserContext.getUserId();
        String roleCode = UserContext.getRoleCode();

        // 只允许超级管理员、分院管理员审核
        if (!RoleConstant.SUPER_ADMIN.equals(roleCode) && !RoleConstant.DEPT_ADMIN.equals(roleCode)) {
            throw new ForbiddenException("仅管理员可执行课程审核操作");
        }

        CourseVO courseVO = courseMapper.selectCourseById(auditDTO.getCourseId());
        if (courseVO == null) {
            return false;
        }
        // 审核通过 → 状态2；驳回退回草稿0
        Course updateCourse = new Course();
        updateCourse.setCourseId(auditDTO.getCourseId());
        updateCourse.setVersion(courseVO.getVersion());
        if (auditDTO.getAuditStatus() == 1) {
            updateCourse.setStatus(2);
        } else {
            updateCourse.setStatus(0);
        }
        int affect = courseMapper.updateCourseStatus(updateCourse);
        if (affect <= 0) {
            return false;
        }
        // 新增审核流水记录
        CourseAudit auditRecord = new CourseAudit();
        auditRecord.setCourseId(auditDTO.getCourseId());
        auditRecord.setAuditorId(loginUserId);
        auditRecord.setAuditStatus(auditDTO.getAuditStatus());
        auditRecord.setAuditRemark(auditDTO.getAuditRemark());
        courseAuditMapper.insert(auditRecord);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyCourse(CourseCopyDTO dto) {
        Long operatorId = UserContext.getUserId();
        String roleCode = UserContext.getRoleCode();
        // 只允许任课教师 teacher
        if (!RoleConstant.TEACHER.equals(roleCode)) {
            throw new ForbiddenException("仅任课教师可以复制课程");
        }

        CourseVO sourceCourse = courseMapper.selectSourceCourse(dto.getSourceCourseId());
        if (sourceCourse == null) {
            throw new NotFoundCourseException("源课程不存在，无法执行复制");
        }

        // dto有值用dto，null就复用源课程
        if (dto.getCourseName() == null) dto.setCourseName(sourceCourse.getCourseName());
        if (dto.getCourseCode() == null) dto.setCourseCode(sourceCourse.getCourseCode());
        if (dto.getDeptId() == null) dto.setDeptId(sourceCourse.getDeptId());
        if (dto.getSemester() == null) dto.setSemester(sourceCourse.getSemester());
        if (dto.getIsPublic() == null) dto.setIsPublic(sourceCourse.getIsPublic());
        if(dto.getDescription() == null) dto.setDescription(sourceCourse.getDescription());

        Course newCourse = new Course();
        BeanUtils.copyProperties(dto, newCourse);
        newCourse.setTeacherId(operatorId);
        //复制出来默认草稿状态
        newCourse.setStatus(0);
        newCourse.setVersion(0);
        courseMapper.insert(newCourse);
        Long newCourseId = newCourse.getCourseId();

        // ==========复制章节以及下属资源==========
        List<Chapter> sourceChapterList = chapterMapper.selectChapterByCourseId(dto.getSourceCourseId());
        // key:旧chapterId  value:新生成chapterId，维护父子关系
        Map<Long, Long> oldToNewChapterIdMap = new HashMap<>();

        //第一轮插入所有章节，parentId先置空
        for (Chapter sourceChapter : sourceChapterList) {
            Chapter newChapter = new Chapter();
            BeanUtils.copyProperties(sourceChapter, newChapter);
            newChapter.setChapterId(null);
            newChapter.setCourseId(newCourseId);
            newChapter.setParentId(null);
            chapterMapper.insert(newChapter);
            oldToNewChapterIdMap.put(sourceChapter.getChapterId(), newChapter.getChapterId());
        }

        //回填父子parentId
        for (Chapter sourceChapter : sourceChapterList) {
            Long oldChapterId = sourceChapter.getChapterId();
            Long oldParentId = sourceChapter.getParentId();
            if(oldParentId != null){
                Long newChapterId = oldToNewChapterIdMap.get(oldChapterId);
                Long newParentId = oldToNewChapterIdMap.get(oldParentId);
                chapterMapper.updateParentId(newChapterId, newParentId);
            }
        }

        //复制章节对应的资源
        for (Map.Entry<Long,Long> entry : oldToNewChapterIdMap.entrySet()){
            Long oldChapterId = entry.getKey();
            Long newChapterId = entry.getValue();
            List<ContentResource> resourceList = resourceMapper.selectByChapterId(oldChapterId);
            for(ContentResource res : resourceList){
                ContentResource newRes = new ContentResource();
                BeanUtils.copyProperties(res,newRes);
                newRes.setResourceId(null);
                newRes.setChapterId(newChapterId);
                resourceMapper.insert(newRes);
            }
        }
        return newCourseId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCourse(CourseUpdateDTO dto) {
        Long loginUserId = UserContext.getUserId();
        String roleCode = UserContext.getRoleCode();
        CourseVO courseVO = courseMapper.selectCourseById(dto.getCourseId());
        if(courseVO == null){
            throw new NotFoundCourseException("课程不存在");
        }
        //权限：课程创建教师 或者管理员
        if(!courseVO.getTeacherId().equals(loginUserId)
                &&!RoleConstant.SUPER_ADMIN.equals(roleCode)
                &&!RoleConstant.DEPT_ADMIN.equals(roleCode)){
            throw new ForbiddenException("仅课程创建教师或者管理员可以编辑课程");
        }
        Course course = new Course();
        BeanUtils.copyProperties(dto,course);
        courseMapper.update(course);
        return Result.success();
    }
}