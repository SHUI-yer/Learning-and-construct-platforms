package com.learning.lpcourse.service.impl;

import com.learning.lpcommon.exception.NotFoundChapterException;
import com.learning.lpcommon.exception.NotFoundCourseException;
import com.learning.lpcommon.exception.NullException;
import com.learning.lpcourse.dto.ChapterDTO;
import com.learning.lpcourse.dto.ChapterSortDTO;
import com.learning.lpcourse.entity.Chapter;
import com.learning.lpcourse.entity.ContentResource;
import com.learning.lpcourse.mapper.ChapterMapper;
import com.learning.lpcourse.mapper.ContentResourceMapper;
import com.learning.lpcourse.mapper.CourseMapper;
import com.learning.lpcourse.service.ChapterService;
import com.learning.lpcourse.vo.ChapterTreeVO;
import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.result.Result;
import com.learning.lpcommon.utils.UserContext;
import com.learning.lpcourse.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final ContentResourceMapper resourceMapper;
    private final CourseMapper courseMapper;

    /**
     * 校验操作章节权限
     * @param courseId 课程ID
     */
    private void checkChapterOperatePermission(Long courseId) {
        String role = UserContext.getRoleCode();
        Long loginUserId = UserContext.getUserId();

        //管理员直接放行
        if (RoleConstant.SUPER_ADMIN.equals(role) || RoleConstant.DEPT_ADMIN.equals(role)) {
            return;
        }
        //教师角色：必须是课程创建者
        if (RoleConstant.TEACHER.equals(role)) {
            CourseVO courseVO = courseMapper.selectCourseById(courseId);
            if (courseVO == null) {
                throw new NotFoundCourseException("课程不存在");
            }
            if (!courseVO.getTeacherId().equals(loginUserId)) {
                throw new ForbiddenException("您不是该课程的授课教师，禁止操作此课程章节");
            }
            return;
        }
        //其他角色直接禁止
        throw new ForbiddenException("无权限操作章节");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addChapter(ChapterDTO dto) {
        //1.校验课程是否存在
        Long courseId = dto.getCourseId();
        CourseVO courseVO = courseMapper.selectCourseById(courseId);
        if (courseVO == null) {
            throw new NotFoundCourseException("课程不存在，无法新增章节");
        }
        //2.权限校验
        checkChapterOperatePermission(courseId);

        Chapter chapter = new Chapter();
        BeanUtils.copyProperties(dto, chapter);
        chapterMapper.insert(chapter);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateChapter(ChapterDTO dto) {
        //根据chapterId查询原有章节，拿到courseId
        Chapter oldChapter = chapterMapper.selectById(dto.getChapterId());
        if (oldChapter == null) {
            throw new NotFoundChapterException("章节不存在");
        }
        //权限校验，拿数据库里真实的courseId，防止DTO被篡改courseId
        checkChapterOperatePermission(oldChapter.getCourseId());

        Chapter chapter = new Chapter();
        BeanUtils.copyProperties(dto, chapter);
        chapterMapper.update(chapter);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteChapter(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new NotFoundChapterException("章节不存在");
        }
        //权限校验
        checkChapterOperatePermission(chapter.getCourseId());

        // 判断该章节是否存在子章节
        List<Chapter> allChapter = chapterMapper.selectFlatByCourseId(chapter.getCourseId());
        boolean hasChild = allChapter.stream()
                .anyMatch(ch -> chapterId.equals(ch.getParentId()));
        if (hasChild) {
            throw new ForbiddenException("该章节下存在子章节，请先删除子章节");
        }
        chapterMapper.deleteById(chapterId);
        return Result.success();
    }

    @Override
    public Result<List<ChapterTreeVO>> getChapterTree(Long courseId) {
        //查询树形不需要强权限，教师、学生、管理员都可以查看
        List<Chapter> flatList = chapterMapper.selectFlatByCourseId(courseId);
        if (flatList.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Long> chapterIdList = flatList.stream().map(Chapter::getChapterId).collect(Collectors.toList());
        List<ContentResource> allResource = resourceMapper.selectByChapterIdList(chapterIdList);
        Map<Long, List<ContentResource>> resourceMap = allResource.stream()
                .collect(Collectors.groupingBy(ContentResource::getChapterId));

        List<ChapterTreeVO> voAll = flatList.stream().map(ch -> {
            ChapterTreeVO vo = new ChapterTreeVO();
            BeanUtils.copyProperties(ch, vo);
            vo.setResourceList(resourceMap.getOrDefault(ch.getChapterId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        Map<Long, List<ChapterTreeVO>> childrenMap = voAll.stream()
                .collect(Collectors.groupingBy(item -> item.getParentId() == null ? -1L : item.getParentId()));
        for (ChapterTreeVO vo : voAll) {
            Long pid = vo.getChapterId();
            vo.setChildren(childrenMap.getOrDefault(pid, Collections.emptyList()));
        }
        List<ChapterTreeVO> treeRoot = childrenMap.getOrDefault(-1L, Collections.emptyList());
        return Result.success(treeRoot);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> sortChapter(ChapterSortDTO sortDTO) {
        Long chapterId = sortDTO.getChapterId();
        Integer targetSortOrder = sortDTO.getTargetSortOrder();

        //参数校验
        if(targetSortOrder == null || targetSortOrder < 0){
            throw new NullException("排序序号不能为负数");
        }
        if(chapterId == null){
            throw new NullException("章节id不能为空");
        }

        //查询待移动的源章节
        Chapter sourceChapter = chapterMapper.selectById(chapterId);
        if(sourceChapter == null){
            throw new NotFoundChapterException("章节不存在");
        }
        Long courseId = sourceChapter.getCourseId();
        Long parentId = sourceChapter.getParentId();

        //权限校验：只允许本课程教师、管理员操作
        checkChapterOperatePermission(courseId);

        //拿到全部同级章节（数据库原始同级列表）
        List<Chapter> siblingList = chapterMapper.selectSiblingChapterList(courseId, parentId);
        if(siblingList.size() <= 1){
            //只有自己，无需排序
            return Result.success();
        }

        // 1.移除要移动的那一条
        Chapter moveItem = null;
        List<Chapter> restList = new ArrayList<>();
        for (Chapter ch : siblingList) {
            if(ch.getChapterId().equals(chapterId)){
                moveItem = ch;
            }else{
                restList.add(ch);
            }
        }

        // 2.在目标位置插入
        restList.add(targetSortOrder, moveItem);

        //3.重新批量写回sortOrder，从0开始连续编号
        for(int i=0;i<restList.size();i++){
            Chapter ch = restList.get(i);
            chapterMapper.updateSortOrder(ch.getChapterId(), i);
        }

        return Result.success();
    }
}