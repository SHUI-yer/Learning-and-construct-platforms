package com.learning.assessment.mapper;

import com.learning.assessment.entity.CheatLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CheatLogMapper {

    @Insert("INSERT INTO cheat_log (submission_id, cheat_type, detail, occurred_at, created_at, updated_at) " +
            "VALUES (#{submissionId}, #{cheatType}, #{detail}, #{occurredAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CheatLog cheatLog);

    @Select("SELECT * FROM cheat_log WHERE id = #{id}")
    CheatLog selectById(Long id);

    @Select("SELECT * FROM cheat_log WHERE submission_id = #{submissionId} ORDER BY occurred_at ASC")
    List<CheatLog> selectBySubmissionId(Long submissionId);

    @Select("SELECT * FROM cheat_log WHERE submission_id = #{submissionId} AND cheat_type = #{cheatType} ORDER BY occurred_at DESC")
    List<CheatLog> selectBySubmissionIdAndType(@Param("submissionId") Long submissionId, @Param("cheatType") String cheatType);

    @Select("SELECT COUNT(*) FROM cheat_log WHERE submission_id = #{submissionId}")
    int countBySubmissionId(Long submissionId);

    @Select("SELECT COUNT(*) FROM cheat_log WHERE submission_id = #{submissionId} AND cheat_type = #{cheatType}")
    int countBySubmissionIdAndType(@Param("submissionId") Long submissionId, @Param("cheatType") String cheatType);
}
