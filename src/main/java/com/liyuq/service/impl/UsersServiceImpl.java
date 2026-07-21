package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liyuq.DTO.RegisterDto;
import com.liyuq.DTO.UserDto;
import com.liyuq.DTO.UserInfoDto;
import com.liyuq.VO.UserCenterVo;
import com.liyuq.VO.UserVo;
import com.liyuq.common.Result;
import com.liyuq.common.UserContext;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.entity.Users;
import com.liyuq.mapper.UsersMapper;
import com.liyuq.service.UsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 鐢ㄦ埛琛 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    @Autowired
    private UsersMapper usersMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Autowired
    private UserContext userContext;

    public Result<Void> register(RegisterDto registerDto){
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();

       wrapper.eq(Users::getUsername, registerDto.getUsername());
        List<Users> list=usersMapper.selectList(wrapper);
        if(list.size()>0){
            return Result.fail(400,"用户已存在");
        }
        Users user=new Users();
        user.setUsername(registerDto.getUsername());
        user.setPassword(ENCODER.encode(registerDto.getPassword()));
        user.setNickname(registerDto.getNickname() != null ? registerDto.getNickname() : registerDto.getUsername());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        usersMapper.insert(user);
        return Result.success();
    }

    @Override
    public Result<UserVo> login(UserDto userDto) {
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getUsername,userDto.getUsername());
        Users user = usersMapper.selectOne(wrapper);
        // ── 第二段：两种失败合并成一句提示 ──
        // matches(明文, 密文)：参数顺序不能反！
        // || 有短路特性：user 为 null 时直接判定失败，不会执行后半句（所以不会空指针）
        if (user == null || !ENCODER.matches(userDto.getPassword(), user.getPassword())) {
            // 文档 3.2：统一提示，不让外人探测出"这个账号存在但密码错了"
            return Result.fail(422, "账号或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        UserVo vo = new UserVo();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        vo.setUserId(user.getId());
        return Result.success(vo);
    }

    @Override
    public UserCenterVo LoadUserInfo() {
        Long userId=UserContext.getUserContextId();
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getId,userId);
       Users users= usersMapper.selectOne(wrapper);
        UserCenterVo vo = new UserCenterVo();
        vo.setId(userId);
        vo.setNickname(users.getNickname());
        vo.setUsername(users.getUsername());
        vo.setCreatedAt(users.getCreatedAt());   // 注册时间也要搬进VO，漏了这行就返回null
        return vo;

    }

    @Override
    public Integer UpdateUserInfo(UserInfoDto userInfoDto) {
        Long userId=UserContext.getUserContextId();
        Users users=new Users();
        users.setId(userId);
        users.setNickname(userInfoDto.getNickname());
        users.setUpdatedAt(LocalDateTime.now());
             int b=usersMapper.updateById(users);

        return b;
    }


}
