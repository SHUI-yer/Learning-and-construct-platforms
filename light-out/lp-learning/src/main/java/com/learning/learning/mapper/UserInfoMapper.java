package com.learning.learning.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserInfoMapper {

    @Select("<script>" +
            "SELECT user_id AS userId, real_name AS realName, avatar_url AS avatarUrl " +
            "FROM user WHERE user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectUserInfoBatch(@Param("userIds") List<Long> userIds);
}
