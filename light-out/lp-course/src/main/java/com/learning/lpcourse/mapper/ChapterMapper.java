package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterMapper {
    void insert(Chapter chapter);
    void update(Chapter chapter);
    void deleteById(Long chapterId);
    Chapter selectById(Long chapterId);
    /** 根据课程id查询该课程全部章节扁平列表，Java内存组装树形 */
    List<Chapter> selectFlatByCourseId(Long courseId);
    /**
     * 根据课程id查询全部章节
     */
    List<Chapter> selectChapterByCourseId(Long courseId);

    /**
     * 更新章节parentId
     */
    void updateParentId(@Param("chapterId") Long chapterId, @Param("parentId") Long parentId);

    /** 更新章节sortOrder */
    void updateSortOrder(@Param("chapterId") Long chapterId, @Param("sortOrder") Integer sortOrder);

    /** 查询同一父节点下全部同级章节 */
    List<Chapter> selectSiblingChapterList(@Param("courseId") Long courseId, @Param("parentId") Long parentId);

}