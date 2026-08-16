package com.liyuq.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.entity.UserOauthBinging;
import com.liyuq.entity.Users;
import com.liyuq.mapper.UserOauthBingingMapper;
import com.liyuq.mapper.UsersMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserOauthBingingMapper userOauthBingingMapper;
    private final UsersMapper usersMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${oauth.frontend-callback-url:http://localhost:5174/oauth/github/callback}")
    private String frontendCallbackUrl;

    public OAuth2LoginSuccessHandler(
            UserOauthBingingMapper userOauthBingingMapper,
            UsersMapper usersMapper,
            JwtUtil jwtUtil,
            ObjectMapper objectMapper) {
        this.userOauthBingingMapper = userOauthBingingMapper;
        this.usersMapper = usersMapper;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }
@Transactional(rollbackFor = Exception.class)
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String githubId = String.valueOf(oauthUser.<Object>getAttribute("id"));
        String login = oauthUser.getAttribute("login");
        String email = oauthUser.getAttribute("email");
        String avatar = oauthUser.getAttribute("avatar_url");

        UserOauthBinging binding = userOauthBingingMapper.selectOne(
                new QueryWrapper<UserOauthBinging>()
                        .eq("provider", "github")
                        // QueryWrapper 里写的是"列名"不是字段名，要和建表的 provider_uid 一字不差
                        .eq("provider_uid", githubId));

            Users user = null;
            if (binding != null) {
                user = usersMapper.selectById(binding.getUserId());
            }

            if (user == null) {
                user = new Users();
                user.setUsername("gh_" + login + "_" + githubId);
                user.setNickname(login);

                // 邮箱冲突就不写。这里是登录流程，不能因为一个附赠字段把人挡在门外；
                // 想绑邮箱有 BingEmail 接口，那里才该抛 409
                Users emailOwner = (email == null) ? null : usersMapper.selectOne(
                        new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
                user.setEmail(emailOwner == null ?email:null);
                user.setAvatar(avatar);
                user.setPassword("");      // OAuth 用户无密码，不能让 null 进 NOT NULL 列
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                usersMapper.insert(user);

                UserOauthBinging newBinding = new UserOauthBinging();
                newBinding.setUserId(user.getId());
                newBinding.setProvider("github");
                newBinding.setProviderUid(githubId);
                newBinding.setCreatedAt(LocalDateTime.now());
                userOauthBingingMapper.insert(newBinding);
            }

            String token = jwtUtil.generateToken(user.getId());
            user.setPassword(null);

            String userJson = objectMapper.writeValueAsString(user);
            String userPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(userJson.getBytes(StandardCharsets.UTF_8));
            String target = frontendCallbackUrl + "#token=" + token + "&user=" + userPayload;

            response.sendRedirect(target);
        }
    }
