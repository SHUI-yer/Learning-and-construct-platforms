package com.learning.lpauth.mapper;

import com.learning.lpauth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {

    @Select("select * from role where role_id = #{roleId}")
    Role selectRoleById(Integer roleId);
}
