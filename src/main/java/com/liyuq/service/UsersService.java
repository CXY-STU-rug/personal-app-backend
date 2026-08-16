package com.liyuq.service;

import com.liyuq.DTO.*;
import com.liyuq.VO.UserCenterVo;
import com.liyuq.VO.UserVo;
import com.liyuq.common.Result;
import com.liyuq.entity.Users;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 鐢ㄦ埛琛 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
public interface UsersService extends IService<Users> {

    Result<Void> register(RegisterDto registerDto);

    Result<UserVo> login(UserDto userDto);

    UserCenterVo LoadUserInfo();

    Integer UpdateUserInfo(UserInfoDto userInfoDto);

    void UpadtePassword(UpdatePassWordDto dto);

    void UpdateAvatar(String avatar);

    void BingEmail(EmailBodyDto dto);

    // 忘记密码：按邮箱发重置链接（旧的 ForgotPassword 已废弃删除，统一走这个）
    void forgotPassword(String email);

    // 参数收成 DTO：token 和明文新密码原来走 @RequestParam 挂在 URL 上，
    // 会被 access log / 浏览器历史 / Referer 原文记录，改走请求体
    void reset(ResetPasswordDto dto);

    void delete();
}
