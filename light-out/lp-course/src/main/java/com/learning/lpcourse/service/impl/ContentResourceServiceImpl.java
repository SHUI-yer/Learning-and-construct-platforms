package com.learning.lpcourse.service.impl;

import com.learning.lpcommon.exception.NotFoundChapterException;
import com.learning.lpcommon.exception.NotFoundCourseException;
import com.learning.lpcourse.dto.ContentResourceDTO;
import com.learning.lpcourse.entity.Chapter;
import com.learning.lpcourse.entity.ContentResource;
import com.learning.lpcourse.mapper.ChapterMapper;
import com.learning.lpcourse.mapper.ContentResourceMapper;
import com.learning.lpcourse.mapper.CourseMapper;
import com.learning.lpcourse.service.ContentResourceService;
import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.result.Result;
import com.learning.lpcommon.utils.UserContext;
import com.learning.lpcourse.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentResourceServiceImpl implements ContentResourceService {

    private final ContentResourceMapper resourceMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    /**
     * 校验资源所属课程操作权限
     */
    private void checkResourceOperatePermission(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new NotFoundChapterException("章节不存在");
        }
        Long courseId = chapter.getCourseId();
        CourseVO courseVO = courseMapper.selectCourseById(courseId);
        if (courseVO == null) {
            throw new NotFoundCourseException("课程不存在");
        }

        String role = UserContext.getRoleCode();
        Long loginUserId = UserContext.getUserId();
        //管理员放行
        if (RoleConstant.SUPER_ADMIN.equals(role) || RoleConstant.DEPT_ADMIN.equals(role)) {
            return;
        }
        //教师：只能操作自己创建的课程
        if (RoleConstant.TEACHER.equals(role)) {
            if (!courseVO.getTeacherId().equals(loginUserId)) {
                throw new ForbiddenException("您不是该课程授课教师，禁止操作该资源");
            }
            return;
        }
        throw new ForbiddenException("无权限操作课程资源");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addResource(ContentResourceDTO dto) {
        //新增：根据传入chapterId校验权限
        checkResourceOperatePermission(dto.getChapterId());
        ContentResource resource = new ContentResource();
        BeanUtils.copyProperties(dto, resource);
        resourceMapper.insert(resource);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateResource(ContentResourceDTO dto) {
        //从数据库读取旧数据，不要用dto里的chapterId，防止前端篡改
        ContentResource oldResource = resourceMapper.selectById(dto.getResourceId());
        if (oldResource == null) {
            throw new NotFoundChapterException("资源不存在");
        }
        checkResourceOperatePermission(oldResource.getChapterId());

        ContentResource resource = new ContentResource();
        BeanUtils.copyProperties(dto, resource);
        resourceMapper.update(resource);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteResource(Long resourceId) {
        ContentResource oldResource = resourceMapper.selectById(resourceId);
        if (oldResource == null) {
            throw new NotFoundChapterException("资源不存在");
        }
        checkResourceOperatePermission(oldResource.getChapterId());

        resourceMapper.deleteById(resourceId);
        return Result.success();
    }
}