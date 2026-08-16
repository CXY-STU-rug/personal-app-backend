package com.liyuq.security;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyuq.entity.Users;
import com.liyuq.mapper.UsersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
class LoginUserDetailsService implements UserDetailsService {


@Autowired
    private UsersMapper usersMapper;


    @Override
    public UserDetails loadUserByUsername(String username)  {

LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<Users>();
queryWrapper.eq(Users::getUsername, username);

        Users user=usersMapper.selectOne(queryWrapper);
        if (user == null) {
            // 查不到用户。DaoAuthenticationProvider 默认 hideUserNotFoundExceptions=true，
            // 会把这个异常悄悄转成 BadCredentialsException，跟"密码错"统一，天然防用户名枚举。
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        return new LoginUser(user);
    }
}
