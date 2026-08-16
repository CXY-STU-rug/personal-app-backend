package com.liyuq.config;

import com.liyuq.common.util.ResponseWriter;
import com.liyuq.security.JwtAuthenticationFilter;
import com.liyuq.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final ResponseWriter responseWriter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          ResponseWriter responseWriter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.responseWriter = responseWriter;
    }
    // 构造器注入。这三个都标了 @Component，Spring 启动时自动塞进来。
    // 原来的写法直接在 filterChain 里用 jwtAuthFilter / oAuth2LoginSuccessHandler，
    // 但类里既没字段也没注入，编译期就是"找不到符号"。

    /**
     * 密码加密器。注册成 Bean 后：
     *   ① DaoAuthenticationProvider 自动用它比对登录密码；
     *   ② 注册 / 找回密码时也用它加密。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 把 Security 内部组装好的 AuthenticationManager 暴露成 Bean，
     * 登录接口才能 @Autowired 进去调 authenticate()。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 跨域配置。
     * ⚠️ 关键点：Security 接管后，WebMvcConfigurer.addCorsMappings 那套对被过滤器链拦下的请求不起作用——
     * 浏览器的预检 OPTIONS 不带 token，会先被 Security 判定"未登录"打回，根本走不到 SpringMVC。
     * 所以跨域必须以 CorsConfigurationSource Bean 的形式交给 Security 自己的 CorsFilter。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // CORS 按字符串精确比对：localhost 和 127.0.0.1 是两个不同来源，都得列上
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173", "http://127.0.0.1:5173",   // uniapp 版前端
                "http://localhost:5174", "http://127.0.0.1:5174"    // 网页版前端
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));   // 允许携带任意请求头（含 Authorization）

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);   // 对哪些路径开放跨域
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))   // 把上面那份跨域规则挂进过滤器链
                .csrf(csrf -> csrf.disable())            // 纯 token 的无状态 API，不用 CSRF
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 不建 session

                .authorizeHttpRequests(auth -> auth
                        // ↓ 白名单：登录/注册/找回密码，这些人本来就还没有 token
                        .requestMatchers(
                                "/api/user/login", "/api/user/register",
                                // 忘记密码的人根本登录不了，这两条不放行等于功能作废（老 WebConfig 就漏了）
                                "/api/user/password/forgot", "/api/user/password/reset",
                                // ↓ 必须和下面 oauth2Login 自定义的两个 baseUri 前缀一致，
                                //   否则第三方登录的入口会被自己拦成 401，一次都跳不出去
                                "/auth/oauth2/**", "/auth/login/oauth2/**","/login"
                        ).permitAll()

                        // ↓ V5 公开帖：游客不登录也能看，只放 GET，写操作照样要登录
                        //   /api/post/public    公开帖列表(3.36)
                        //   /api/post/detail    帖子详情(3.37)  —— 私密帖在 service 里再判作者
                        //   /api/post/hot       热门帖
                        //   /api/post/user/**   某作者主页(3.38)
                        //   /api/post/comments/**  帖子评论列表(3.40)
                        //   这里逐条列而不写成 /api/post/**：以后新增私有 GET 接口不会被误放行
                        .requestMatchers(HttpMethod.GET,
                                "/api/post/public", "/api/post/detail", "/api/post/hot",
                                "/api/post/user/**", "/api/post/comments/**"
                        ).permitAll()

                        .anyRequest().authenticated())        // 其余一律要登录

                // ⚠️ 必须自定义未登录的处理：一旦开了 oauth2Login，默认入口点会把未登录的请求
                //    302 重定向到 GitHub 授权页，前端 fetch 拿到的是一坨 HTML 而不是 401 JSON
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, resp, ex) -> responseWriter.writeResponse(resp, 401, "用户未登录")))

                .oauth2Login(oauth -> oauth
                        // 前端跳这个地址发起登录：/auth/oauth2/authorization/github
                        .authorizationEndpoint(a -> a.baseUri("/auth/oauth2/authorization"))
                        // GitHub 回调打到这个地址：/auth/login/oauth2/code/github
                        // （要和 yml 的 redirect-uri、GitHub OAuth App 里填的 callback URL 三方一致）
                        .redirectionEndpoint(r -> r.baseUri("/auth/login/oauth2/code/*"))
                        .successHandler(oAuth2LoginSuccessHandler))   // 挂上 3.C.3：建号 + 签自家 JWT + 回跳前端

                // ↓ 把自签 JWT 校验过滤器塞在用户名密码过滤器之前：
                //   带 token 的请求在这里就认好身份，后面 authorizeHttpRequests 才知道你已登录
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
