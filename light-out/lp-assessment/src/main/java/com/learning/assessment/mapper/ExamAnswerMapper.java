package com.learning.assessment.mapper;

import com.learning.assessment.entity.ExamAnswer;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExamAnswerMapper {

    @Insert("INSERT INTO exam_answer (submission_id, q_id, student_answer, is_correct, score, " +
            "answered_at, created_at, updated_at) " +
            "VALUES (#{submissionId}, #{qId}, #{studentAnswer}, #{isCorrect}, #{score}, " +
            "#{answeredAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamAnswer examAnswer);

    @Update("UPDATE exam_answer SET is_correct = #{isCorrect}, score = #{score}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(ExamAnswer examAnswer);

    @Select("SELECT * FROM exam_answer WHERE id = #{id}")
    ExamAnswer selectById(Long id);

    @Select("SELECT * FROM exam_answer WHERE submission_id = #{submissionId} ORDER BY answered_at ASC")
    List<ExamAnswer> selectBySubmissionId(Long submissionId);

    @Select("SELECT * FROM exam_answer WHERE submission_id = #{submissionId} AND q_id = #{qId}")
    ExamAnswer selectBySubmissionAndQuestion(@Param("submissionId") Long submissionId, @Param("qId") Long qId);

    @Select("SELECT COUNT(*) FROM exam_answer WHERE submission_id = #{submissionId} AND is_correct = 1")
    int countCorrectBySubmissionId(Long submissionId);

    @Delete("DELETE FROM exam_answer WHERE submission_id = #{submissionId}")
    int deleteBySubmissionId(Long submissionId);
}
