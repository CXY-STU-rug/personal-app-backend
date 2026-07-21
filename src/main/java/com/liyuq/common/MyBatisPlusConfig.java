package com.liyuq.common;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 参数：数据库类型 DbType.MYSQL
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 可选配置
        paginationInnerInterceptor.setOverflow(false); // pageNum超过最大页是否返回第一页
        paginationInnerInterceptor.setMaxLimit(500L); // 单页最大条数限制
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
