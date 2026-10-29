package com.learning.lpauth.mapper;

import com.learning.lpauth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper {

    @Select("select * from user_role where user_id = #{userId}")
    UserRole selectUserRoleByUserId(Long userId);
}
