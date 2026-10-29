package com.learning.assessment.mapper;

import com.learning.assessment.entity.PaperQuestion;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PaperQuestionMapper {

    @Insert("INSERT INTO paper_question (paper_id, q_id, score, sort_order, created_at, updated_at) " +
            "VALUES (#{paperId}, #{qId}, #{score}, #{sortOrder}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PaperQuestion paperQuestion);

    @Delete("DELETE FROM paper_question WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM paper_question WHERE paper_id = #{paperId}")
    int deleteByPaperId(Long paperId);

    @Update("UPDATE paper_question SET score = #{score}, sort_order = #{sortOrder}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(PaperQuestion paperQuestion);

    @Select("SELECT * FROM paper_question WHERE id = #{id}")
    PaperQuestion selectById(Long id);

    @Select("SELECT * FROM paper_question WHERE paper_id = #{paperId} ORDER BY sort_order ASC")
    List<PaperQuestion> selectByPaperId(Long paperId);

    @Select("SELECT COALESCE(SUM(score), 0) FROM paper_question WHERE paper_id = #{paperId}")
    java.math.BigDecimal sumScoreByPaperId(Long paperId);
}