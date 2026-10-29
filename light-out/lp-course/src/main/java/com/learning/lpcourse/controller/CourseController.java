package com.learning.lpcourse.controller;

import com.learning.lpcourse.dto.*;
import com.learning.lpcourse.service.ChapterService;
import com.learning.lpcourse.service.ContentResourceService;
import com.learning.lpcourse.service.CourseService;
import com.learning.lpcourse.vo.ChapterTreeVO;
import com.learning.lpcourse.vo.CourseVO;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final ChapterService chapterService;
    private final ContentResourceService contentResourceService;

    /**
     * 课程分页列表
     */
    @GetMapping("/page")
    public Result<PageResult<CourseVO>> getCoursePage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            CourseQueryDTO query
    ) {
        PageResult<CourseVO> pageResult = courseService.getCoursePage(pageNum, pageSize, query);
        return Result.success(pageResult);
    }

    /**
     * 课程详情
     */
    @GetMapping("/{courseId}")
    public Result<CourseVO> getCourseInfo(@PathVariable Long courseId) {
        CourseVO vo = courseService.getCourseInfo(courseId);
        return Result.success(vo);
    }

    /**
     * 教师自主创建课程
     */
    @PostMapping("/create")
    public Result<Long> createCourse(@RequestBody CourseCreateDTO dto) {
        Long id = courseService.createCourse(dto);
        return Result.success(id);
    }
    /**
     * 管理员导入创建课程
     */
    @PostMapping("/import")
    public Result<Void> importCourse(@RequestBody List<CourseImportItemDTO> itemList) {
        courseService.importCourse(itemList);
        return Result.success();
    }

    /**
     * 课程审核（管理员）
     */
    @PostMapping("/audit")
    public Result<Boolean> auditCourse(@RequestBody CourseAuditDTO dto) {
        Boolean success = courseService.auditCourse(dto);
        return Result.success(success);
    }

    /**
     * 复制课程
     */
    @PostMapping("/copy")
    public Result<Long> copyCourse(@RequestBody CourseCopyDTO dto) {
        Long newId = courseService.copyCourse(dto);
        return Result.success(newId);
    }


    //============章节接口============
    @PostMapping("/chapter/add")
    public Result<Void> addChapter(@RequestBody ChapterDTO dto){
        return chapterService.addChapter(dto);
    }

    @PutMapping("/chapter/update")
    public Result<Void> updateChapter(@RequestBody ChapterDTO dto){
        return chapterService.updateChapter(dto);
    }

    @DeleteMapping("/chapter/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable Long chapterId){
        return chapterService.deleteChapter(chapterId);
    }

    @GetMapping("/chapter/tree/{courseId}")
    public Result<List<ChapterTreeVO>> getChapterTree(@PathVariable Long courseId){
        return chapterService.getChapterTree(courseId);
    }

    //============资源接口============
    @PostMapping("/resource/add")
    public Result<Void> addResource(@RequestBody ContentResourceDTO dto){
        return contentResourceService.addResource(dto);
    }

    @PutMapping("/resource/update")
    public Result<Void> updateResource(@RequestBody ContentResourceDTO dto){
        return contentResourceService.updateResource(dto);
    }

    @DeleteMapping("/resource/{resourceId}")
    public Result<Void> deleteResource(@PathVariable Long resourceId){
        return contentResourceService.deleteResource(resourceId);
    }

    //========迭代：课程编辑接口========
    @PutMapping("/update")
    public Result<Void> updateCourse(@RequestBody CourseUpdateDTO dto){
        return courseService.updateCourse(dto);
    }

    /**
     * 章节拖拽排序接口
     * PUT /course/chapter/sort
     */
    @PutMapping("/chapter/sort")
    public Result<Void> sortChapter(@RequestBody ChapterSortDTO sortDTO){
        return chapterService.sortChapter(sortDTO);
    }
}