package com.liyuq.security;

import com.liyuq.common.UserContext;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.common.util.ResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
// 头不存在、或者不是 Bearer 开头的，都当没
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = header.substring(7);


            try {
                Long userId = jwtUtil.getUserId(token);
                // 写入 SecurityContext，供授权规则判断
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserContext.SetUserContext(userId);


            } catch (Exception e) {
                // Token 无效/过期 → 统一输出 Result JSON
                SecurityContextHolder.clearContext();
                responseWriter.writeResponse(response, 401, "Token无效或已过期");
                return;
            }
            // 只有解析成功才会走到这里，doFilter 在内层 try 之外
            filterChain.doFilter(request, response);

        }   finally {
            SecurityContextHolder.clearContext();
            UserContext.removeUserContextId();
        }
    }
}