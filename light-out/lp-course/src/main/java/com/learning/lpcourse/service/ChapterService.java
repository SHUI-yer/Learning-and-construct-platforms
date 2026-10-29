package com.learning.lpcourse.service;

import com.learning.lpcourse.dto.ChapterDTO;
import com.learning.lpcourse.dto.ChapterSortDTO;
import com.learning.lpcourse.vo.ChapterTreeVO;
import com.learning.lpcommon.result.Result;
import java.util.List;

public interface ChapterService {
    Result<Void> addChapter(ChapterDTO dto);
    Result<Void> updateChapter(ChapterDTO dto);
    Result<Void> deleteChapter(Long chapterId);
    /** 根据课程ID获取完整树形目录（包含挂载资源） */
    Result<List<ChapterTreeVO>> getChapterTree(Long courseId);
    /**
     * 章节同级拖拽排序
     * @param sortDTO 排序参数
     * @return Result
     */
    Result<Void> sortChapter(ChapterSortDTO sortDTO);
}