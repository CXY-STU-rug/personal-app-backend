package com.liyuq.controller;

import com.liyuq.DTO.UserDto;
import com.liyuq.VO.AuthResponse;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.Result;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.entity.Users;
import com.liyuq.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;



    @RestController
    @RequestMapping("/login")
    public  class OauthController {
        @Autowired
        private JwtUtil jwtUtil;
    
        /**
         * Spring Security 认证入口。
         * 调它的 authenticate() 会自动走：FeignUserDetailsService 查用户 → PasswordEncoder 比对密码 → 禁用检查。
         */
        @Autowired
        private AuthenticationManager authenticationManager;

        @PutMapping()
        public Result<AuthResponse> login(@RequestBody UserDto dto) {

            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
            } catch (DisabledException e) {
                // status==0 被禁用: 保留原来的 403 语义
                throw new BusinessException(403, "账号已被禁用, 请联系管理员");
            } catch (AuthenticationException e) {
                // 用户不存在 / 密码错: 统一提示, 不区分, 防用户名枚举
                throw new BusinessException(404,"用户名或密码错误");
            }
    // ② 认证通过: principal 就是我们的 LoginUser, 取出业务 User 去签自家 JWT
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            Users user = loginUser.getUser();
            String token = jwtUtil.generateToken(user.getId());
            user.setPassword(null);   // 兜底: 密文绝不出网关
            return Result.success(new AuthResponse(token, user));
    
    

        }
    }

