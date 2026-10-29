package com.learning.lpcourse.vo;

import com.learning.lpcourse.entity.ContentResource;
import lombok.Data;
import java.util.List;

@Data
public class ChapterTreeVO {
    private Long chapterId;
    private Long courseId;
    private String title;
    private Integer sortOrder;
    private Long parentId;
    /** 子章节（递归嵌套，支持无限层级，数据库设计支持三级） */
    private List<ChapterTreeVO> children;
    /** 当前章节挂载的资源列表 */
    private List<ContentResource> resourceList;
}