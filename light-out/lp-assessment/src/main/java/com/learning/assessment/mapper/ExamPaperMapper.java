package com.learning.assessment.mapper;

import com.learning.assessment.entity.ExamPaper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExamPaperMapper {

    @Insert("INSERT INTO exam_paper (course_id, task_id, title, total_score, duration, " +
            "shuffle_questions, version, created_at, updated_at) " +
            "VALUES (#{courseId}, #{taskId}, #{title}, #{totalScore}, #{duration}, " +
            "#{shuffleQuestions}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "paperId")
    int insert(ExamPaper examPaper);

    @Delete("DELETE FROM exam_paper WHERE paper_id = #{paperId}")
    int deleteById(Long paperId);

    @Update("UPDATE exam_paper SET title = #{title}, total_score = #{totalScore}, " +
            "duration = #{duration}, shuffle_questions = #{shuffleQuestions}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE paper_id = #{paperId} AND version = #{version}")
    int update(ExamPaper examPaper);

    @Select("SELECT * FROM exam_paper WHERE paper_id = #{paperId}")
    ExamPaper selectById(Long paperId);

    @Select("SELECT * FROM exam_paper WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<ExamPaper> selectByCourseId(Long courseId);

    @Select("SELECT * FROM exam_paper WHERE course_id = #{courseId} ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<ExamPaper> selectByCourseIdPage(@Param("courseId") Long courseId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM exam_paper WHERE course_id = #{courseId}")
    long countByCourseId(Long courseId);

    @Select("SELECT * FROM exam_paper WHERE task_id = #{taskId}")
    ExamPaper selectByTaskId(Long taskId);

    @Update("UPDATE exam_paper SET task_id = #{taskId}, updated_at = NOW() WHERE paper_id = #{paperId}")
    int updateTaskId(@Param("paperId") Long paperId, @Param("taskId") Long taskId);
}
