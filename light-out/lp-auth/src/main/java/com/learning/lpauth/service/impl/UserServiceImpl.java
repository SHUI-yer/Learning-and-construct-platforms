package com.learning.lpauth.service.impl;

import com.learning.lpauth.dto.ChangePasswordDTO;
import com.learning.lpauth.dto.LoginDTO;
import com.learning.lpauth.dto.ResetPasswordDTO;
import com.learning.lpauth.entity.Role;
import com.learning.lpauth.entity.User;
import com.learning.lpauth.entity.UserLoginInfo;
import com.learning.lpauth.entity.UserRole;
import com.learning.lpauth.mapper.RoleMapper;
import com.learning.lpauth.mapper.UserMapper;
import com.learning.lpauth.mapper.UserRoleMapper;
import com.learning.lpauth.security.PasswordHashes;
import com.learning.lpauth.security.PasswordPolicy;
import com.learning.lpauth.service.UserService;
import com.learning.lpcommon.constant.RoleConstant;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.exception.ForbiddenException;
import com.learning.lpcommon.exception.NotFoundRoleException;
import com.learning.lpcommon.exception.NotFoundUserException;
import com.learning.lpcommon.exception.PasswordErrorException;
import com.learning.lpcommon.exception.PasswordResetRequiredException;
import com.learning.lpcommon.exception.UserLockedException;
import com.learning.lpcommon.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录校验与口令管理。
 *
 * <p>口令哈希策略：新口令一律 BCrypt；存量无盐 MD5 由 {@link PasswordHashes} 识别，
 * 默认在登录成功后立刻原子地替换为 BCrypt（旧哈希不再保留）。
 * 若把 {@code security.password.legacy-md5} 配成 {@code reject}，
 * 则存量账号会被要求先由管理员重置密码，不再签发令牌。</p>
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 存量 MD5 账号的处理策略 */
    private static final String POLICY_MIGRATE = "migrate";
    private static final String POLICY_REJECT = "reject";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final String legacyMd5Policy;

    public UserServiceImpl(UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder,
                           @Value("${security.password.legacy-md5:migrate}") String legacyMd5Policy) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.legacyMd5Policy = legacyMd5Policy == null ? POLICY_MIGRATE : legacyMd5Policy.trim().toLowerCase();
        if (!POLICY_MIGRATE.equals(this.legacyMd5Policy) && !POLICY_REJECT.equals(this.legacyMd5Policy)) {
            throw new IllegalStateException(
                    "security.password.legacy-md5 只支持 migrate 或 reject，当前配置为 " + legacyMd5Policy);
        }
    }

    // ========== 登录 ==========

    @Override
    public UserLoginInfo login(LoginDTO loginDTO) {
        if (loginDTO == null
                || loginDTO.getUsername() == null || loginDTO.getUsername().isBlank()
                || loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
            throw new PasswordErrorException("请输入用户名和密码");
        }
        User user = userMapper.selectByUsername(loginDTO.getUsername().trim());
        // 账号不存在
        if (user == null) {
            throw new NotFoundUserException("账号不存在");
        }
        // 密码校验（含存量哈希迁移）
        verifyPassword(user, loginDTO.getPassword());
        // 账号禁用
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new UserLockedException("账号被禁用");
        }
        // 查询角色
        UserRole userRole = userRoleMapper.selectUserRoleByUserId(user.getUserId());
        if (userRole == null) {
            throw new NotFoundRoleException("当前用户未分配角色，禁止登录");
        }
        Role role = roleMapper.selectRoleById(userRole.getRoleId());
        if (role == null) {
            throw new NotFoundRoleException("用户对应的角色不存在");
        }
        UserLoginInfo loginInfo = new UserLoginInfo();
        loginInfo.setUser(user);
        loginInfo.setRole(role);
        return loginInfo;
    }

    // ========== 修改自己的密码 ==========

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ForbiddenException("未登录或登录状态无效，无法修改密码");
        }
        if (dto == null) {
            throw new BaseException("参数不能为空");
        }
        PasswordPolicy.validate(dto.getNewPassword());
        if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
            throw new BaseException("请输入当前密码");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundUserException("账号不存在或已被删除");
        }
        // 校验旧密码：兼容存量 MD5，并顺带完成迁移
        verifyPassword(user, dto.getOldPassword());
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BaseException("新密码不能与当前密码相同");
        }
        writePassword(userId, dto.getNewPassword());
        log.info("用户修改密码成功: userId={}", userId);
    }

    // ========== 管理员重置他人密码 ==========

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        requireAdmin("重置用户密码");
        if (dto == null || dto.getUserId() == null) {
            throw new BaseException("目标用户ID不能为空");
        }
        PasswordPolicy.validate(dto.getNewPassword());
        User target = userMapper.selectById(dto.getUserId());
        if (target == null) {
            throw new NotFoundUserException("目标账号不存在或已被删除");
        }
        writePassword(target.getUserId(), dto.getNewPassword());
        log.info("管理员重置密码成功: operatorId={}, targetUserId={}",
                UserContext.getUserId(), target.getUserId());
    }

    // ========== 内部工具 ==========

    /**
     * 按哈希格式选择校验方式。
     *
     * <ul>
     *   <li>BCrypt：正常校验；若 cost 低于当前配置则顺带升级</li>
     *   <li>存量 MD5：默认校验通过后立即迁移为 BCrypt；reject 策略下要求重置密码</li>
     *   <li>其他格式：直接拒绝，避免把"无法识别的哈希"当成通过条件</li>
     * </ul>
     */
    private void verifyPassword(User user, String rawPassword) {
        String storedHash = user.getPasswordHash();

        if (PasswordHashes.isBcrypt(storedHash)) {
            if (!passwordEncoder.matches(rawPassword, storedHash)) {
                throw new PasswordErrorException("密码错误");
            }
            if (passwordEncoder.upgradeEncoding(storedHash)) {
                rewriteHash(user, rawPassword);
            }
            return;
        }

        if (PasswordHashes.isLegacyMd5(storedHash)) {
            if (!PasswordHashes.md5Matches(rawPassword, storedHash)) {
                throw new PasswordErrorException("密码错误");
            }
            if (POLICY_REJECT.equals(legacyMd5Policy)) {
                throw new PasswordResetRequiredException("口令算法已升级，请联系管理员重置密码后再登录");
            }
            // 迁移：写入 BCrypt 并覆盖旧哈希，旧 MD5 值不再保留
            rewriteHash(user, rawPassword);
            log.info("存量 MD5 口令已迁移为 BCrypt: userId={}", user.getUserId());
            return;
        }

        log.warn("账号 {} 的口令哈希格式无法识别，已拒绝登录", user.getUserId());
        throw new PasswordResetRequiredException("账号口令信息异常，请联系管理员重置密码");
    }

    /** 统一写入口：BCrypt 编码后覆写，0 行表示账号已被删除 */
    private void writePassword(Long userId, String rawPassword) {
        writeEncoded(userId, passwordEncoder.encode(rawPassword));
    }

    /** 重新编码并覆写哈希，同时同步内存对象（登录迁移与 cost 升级复用） */
    private void rewriteHash(User user, String rawPassword) {
        String encoded = passwordEncoder.encode(rawPassword);
        writeEncoded(user.getUserId(), encoded);
        user.setPasswordHash(encoded);
    }

    private void writeEncoded(Long userId, String encoded) {
        int rows = userMapper.updatePasswordHash(userId, encoded);
        if (rows == 0) {
            throw new BaseException("密码更新失败：账号不存在或已被删除");
        }
    }

    private void requireAdmin(String action) {
        String role = UserContext.getRoleCode();
        boolean admin = RoleConstant.SUPER_ADMIN.equals(role) || RoleConstant.DEPT_ADMIN.equals(role);
        if (!admin) {
            throw new ForbiddenException("仅管理员可" + action);
        }
    }
}
