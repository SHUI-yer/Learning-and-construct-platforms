package com.learning.lpauth.mapper;

import com.learning.lpauth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    /**
     * 按用户名查询账号。逻辑删除的账号不返回，避免已注销用户仍能登录。
     */
    @Select("select * from user where username = #{username} and deleted_at is null")
    User selectByUsername(String username);

    /**
     * 按主键查询账号（同样过滤逻辑删除）。
     */
    @Select("select * from user where user_id = #{userId} and deleted_at is null")
    User selectById(@Param("userId") Long userId);

    /**
     * 覆写口令哈希。
     *
     * <p>用于三件事：① 新口令一律按 BCrypt 写入；② 存量 MD5 账号登录成功后
     * 立即把旧哈希替换掉（旧值不保留）；③ 用户改密 / 管理员重置。</p>
     *
     * @return 影响行数，0 表示账号不存在或已被删除
     */
    @Update("update user set password_hash = #{passwordHash}, updated_at = now() "
            + "where user_id = #{userId} and deleted_at is null")
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
