package com.learning.lpcommon.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    // 当前页数据
    private List<T> records;
    // 总条数
    private Long total;
    // 当前页码
    private Long pageNum;
    // 每页条数
    private Long pageSize;
    // 总页数
    private Long pages;

    /**
     * 简化构造：仅数据和总数
     */
    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    /**
     * 静态构建分页对象
     */
    public static <T> PageResult<T> build(List<T> records, Long total, Long pageNum, Long pageSize) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(total);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        if (pageSize == null || pageSize <= 0) {
            pageResult.setPages(0L);
        } else {
            // 向上取整公式
            pageResult.setPages((total + pageSize - 1) / pageSize);
        }
        return pageResult;
    }
}