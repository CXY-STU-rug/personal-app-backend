package com.liyuq.common;

import com.liyuq.common.Exception.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtInterceptor jwtInterceptor;


    public WebConfig(JwtInterceptor jwtInterceptor) {
       this.jwtInterceptor = jwtInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截全部接口
                .addPathPatterns("/**")
                // 放行登录、注册，不用校验token
                .excludePathPatterns("/api/user/login","/api/user/register");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                      // 对哪些路径开放跨域
                // 注意：allowedOrigins 连续调用两次是"覆盖"不是"追加"，
                // 多个来源必须写在同一次调用里，用逗号隔开。
                // 另外 CORS 按字符串精确比对：localhost 和 127.0.0.1 是两个不同来源，都要列上
                .allowedOrigins(
                        "http://localhost:5173", "http://127.0.0.1:5173",  // uniapp 版前端
                        "http://localhost:5174", "http://127.0.0.1:5174"   // 网页版前端
                )
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*");                    // 允许携带任意请求头（含Authorization）
    }

}
