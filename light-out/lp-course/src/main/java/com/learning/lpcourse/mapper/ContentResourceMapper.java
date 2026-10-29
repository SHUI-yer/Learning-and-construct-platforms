package com.learning.lpcourse.mapper;

import com.learning.lpcourse.entity.ContentResource;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ContentResourceMapper {
    void insert(ContentResource resource);
    void update(ContentResource resource);
    void deleteById(Long resourceId);
    List<ContentResource> selectByChapterId(Long chapterId);
    /** 批量查询一批chapterId下面所有资源 */
    List<ContentResource> selectByChapterIdList(List<Long> chapterIdList);

    /** 根据资源id查询单条资源 */
    ContentResource selectById(Long resourceId);
}