package com.learning.assessment.mapper;

import com.learning.assessment.entity.QuestionBank;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QuestionBankMapper {

    @Insert("INSERT INTO question_bank (course_id, creator_id, q_type, content, options, answer, analysis, " +
            "difficulty, tags, source, import_id, version, created_at, updated_at) " +
            "VALUES (#{courseId}, #{creatorId}, #{qType}, #{content}, #{options}, #{answer}, #{analysis}, " +
            "#{difficulty}, #{tags}, #{source}, #{importId}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "qId")
    int insert(QuestionBank questionBank);

    @Delete("DELETE FROM question_bank WHERE q_id = #{qId}")
    int deleteById(Long qId);

    @Update("UPDATE question_bank SET q_type = #{qType}, content = #{content}, options = #{options}, " +
            "answer = #{answer}, analysis = #{analysis}, difficulty = #{difficulty}, tags = #{tags}, " +
            "version = version + 1, updated_at = NOW() WHERE q_id = #{qId} AND version = #{version}")
    int update(QuestionBank questionBank);

    @Select("SELECT * FROM question_bank WHERE q_id = #{qId}")
    QuestionBank selectById(Long qId);

    @SelectProvider(type = QuestionBankSqlProvider.class, method = "selectByConditions")
    List<QuestionBank> selectByConditions(@Param("courseId") Long courseId,
                                          @Param("qType") String qType,
                                          @Param("difficulty") Integer difficulty,
                                          @Param("keyword") String keyword);

    @SelectProvider(type = QuestionBankSqlProvider.class, method = "countByConditions")
    long countByConditions(@Param("courseId") Long courseId,
                           @Param("qType") String qType,
                           @Param("difficulty") Integer difficulty,
                           @Param("keyword") String keyword);

    @SelectProvider(type = QuestionBankSqlProvider.class, method = "selectByConditionsWithLimit")
    List<QuestionBank> selectByConditionsWithLimit(@Param("courseId") Long courseId,
                                                   @Param("qType") String qType,
                                                   @Param("difficulty") Integer difficulty,
                                                   @Param("keyword") String keyword,
                                                   @Param("offset") int offset,
                                                   @Param("pageSize") int pageSize);

    @SelectProvider(type = QuestionBankSqlProvider.class, method = "selectRandomByConditions")
    List<QuestionBank> selectRandomByConditions(@Param("courseId") Long courseId,
                                                @Param("qType") String qType,
                                                @Param("difficulty") Integer difficulty,
                                                @Param("count") int count);

    @Select("SELECT COUNT(*) FROM question_bank WHERE course_id = #{courseId}")
    int countByCourseId(Long courseId);

    class QuestionBankSqlProvider {
        public String buildWhere(Long courseId, String qType, Integer difficulty, String keyword) {
            StringBuilder sql = new StringBuilder(" WHERE 1=1");
            if (courseId != null) {
                sql.append(" AND course_id = #{courseId}");
            }
            if (qType != null && !qType.isEmpty()) {
                sql.append(" AND q_type = #{qType}");
            }
            if (difficulty != null) {
                sql.append(" AND difficulty = #{difficulty}");
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (content LIKE CONCAT('%', #{keyword}, '%') OR analysis LIKE CONCAT('%', #{keyword}, '%'))");
            }
            return sql.toString();
        }

        public String selectByConditions(Long courseId, String qType, Integer difficulty, String keyword) {
            return "SELECT * FROM question_bank" + buildWhere(courseId, qType, difficulty, keyword)
                    + " ORDER BY updated_at DESC";
        }

        public String selectByConditionsWithLimit(Long courseId, String qType, Integer difficulty,
                                                 String keyword, int offset, int pageSize) {
            return "SELECT * FROM question_bank" + buildWhere(courseId, qType, difficulty, keyword)
                    + " ORDER BY updated_at DESC LIMIT #{offset}, #{pageSize}";
        }

        public String countByConditions(Long courseId, String qType, Integer difficulty, String keyword) {
            return "SELECT COUNT(*) FROM question_bank" + buildWhere(courseId, qType, difficulty, keyword);
        }

        public String selectRandomByConditions(Long courseId, String qType, Integer difficulty, int count) {
            return "SELECT * FROM question_bank" + buildWhere(courseId, qType, difficulty, null)
                    + " ORDER BY RAND() LIMIT #{count}";
        }
    }
}
