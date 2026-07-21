package com.liyuq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 两个核心序列化器
        StringRedisSerializer stringSer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer();

        // 4 个位置分别设
        template.setKeySerializer(stringSer);          // 大 key
        template.setValueSerializer(jsonSer);          // String/List/Set/ZSet 的 value
        template.setHashKeySerializer(stringSer);      // Hash 的 field 名
        template.setHashValueSerializer(jsonSer);      // Hash 的 field 值

        template.afterPropertiesSet();                 // 必调! 否则用默认
        return template;
    }
}