package com.liyuq.common.Exception.interceptor;


import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.entity.Users;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, final Object handler) throws Exception {

        String authHeader = request.getHeader("Authorization");

        if ("OPTIONS".equals(request.getMethod())) {
            return true;   // 预检请求不带token，直接放行，交给CORS配置去应答
        }
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Long userId;
        try {
            // 3. 解析token获取用户ID
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            // token过期/篡改/非法
            throw new BusinessException(401, "Token无效或已过期，请重新登录");
        }

        // 4. 封装真实用户ID存入上下文，不要new空Users

        UserContext.SetUserContext(userId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
       UserContext.removeUserContextId();
    }
}