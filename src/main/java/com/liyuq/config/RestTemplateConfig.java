package com.liyuq.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate的Bean定义（V3调第三方API用）
 * 为什么Spring不自动给：超时配多少只有业务方知道，逼你在造Bean时想清楚——
 * 调别人的服务和查自己的库最大的区别就是"别人可能不理你"，必须设死等待上限
 *
 * 底层客户端用Apache HttpClient（不用JDK自带的SimpleClientHttpRequestFactory）：
 * 和风的响应全是gzip压缩，JDK简易客户端不解压会拿到乱码字节；HttpClient自动解压
 */
@Configuration   // 告诉Spring：这个类里有Bean的"制造说明书"，启动时来扫
public class RestTemplateConfig {

    @Bean        // 方法返回值注册进Spring容器；此后@Autowired RestTemplate就能找到它了
    public RestTemplate restTemplate() {
        // 超时配置写在HttpClient自己的RequestConfig里（Spring的工厂类不提供读取超时的设置口）
        RequestConfig timeouts = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))    // 连接超时3秒：握不上手就放弃（对方挂了/网络不通）
                .setResponseTimeout(Timeout.ofSeconds(5))   // 响应超时5秒：连上了但不给数据也放弃（对方卡死）
                .build();
        // 不设这两个 = 无限等。第三方一卡，你首页的天气请求就永远转圈，线程被占满后整个后端瘫痪

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(timeouts)
                .build();   // 造一个带超时的Apache HttpClient（pom里的httpclient5依赖就是给它用的）

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Bean
    public RestTemplate aiRestTemplate() {
        // AI专用的第二把RestTemplate：响应超时放宽到30秒（接口文档3.28的注意事项——
        // 模型思考3~10秒是正常的，复用天气那把5秒的会天天误杀）。
        // 同类型有两个Bean时，Spring按"字段名==Bean方法名"分辨：
        //   @Autowired private RestTemplate restTemplate;   → 拿到上面5秒的
        //   @Autowired private RestTemplate aiRestTemplate; → 拿到这把30秒的
        // 字段名对不上任何Bean名才会报NoUniqueBeanDefinitionException
        RequestConfig timeouts = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))     // 握手还是3秒：连不上没必要多等
                .setResponseTimeout(Timeout.ofSeconds(30))   // 等回答放宽到30秒：AI在"想"
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(timeouts)
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
