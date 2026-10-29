package com.learning.assessment.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperCreateDTO {

    private Long paperId;
    private Long courseId;
    private String title;
    // 限时(分钟)，可为空
    private Integer duration;
    // 是否乱序出题: 1-是 0-否
    private Integer shuffleQuestions;
    // 乐观锁版本号，更新时必填
    private Integer version;
    // 题目项（qId + score），为空则不重建题目
    private List<PaperQuestionItemDTO> items;
}
