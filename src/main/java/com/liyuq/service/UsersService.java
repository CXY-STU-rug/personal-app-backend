package com.liyuq.service;

import com.liyuq.DTO.RegisterDto;
import com.liyuq.DTO.UserDto;
import com.liyuq.DTO.UserInfoDto;
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
}
