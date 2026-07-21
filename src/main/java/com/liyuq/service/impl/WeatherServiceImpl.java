package com.liyuq.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuq.VO.WeatherVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 实时天气实现（契约3.27）：后端当"二道贩子"——
 * 一次 /api/weather/now = 串行调和风3个接口（lookup→now→3d），把第三方字段翻译成我们自己的契约。
 * 调用链和字段翻译表见接口文档3.27的附录。
 */
@Slf4j     // Lombok造一个叫log的日志记录器（和@Data造getter同理），异常转译前先留真相
@Service
public class WeatherServiceImpl implements WeatherService {

    @Value("${weather.api-host}")      // ${}里按yml的层级路径逐字符对暗号，多一个空格都不行
    private String apiHost;

    @Value("${weather.api-key}")
    private String apiKey;

    @Value("${weather.default-city}")
    private String defaultCity;

    @Autowired
    private RestTemplate restTemplate;   // Bean来自RestTemplateConfig，超时3秒/5秒在那里配的

    @Autowired
    private ObjectMapper objectMapper;   // Spring Boot自带的JSON翻译机，直接注入就能用

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public WeatherVo getWeatherNow(String city) {

        // ① city是可选参数：不传/传空串就用yml里配的默认城市兜底
        if (city == null || city.isBlank()) {
            city = defaultCity;
        }
        String weatherRedis="weather"+city;
        Object cached = redisTemplate.opsForValue().get(weatherRedis);
        if (cached != null) {
            return (WeatherVo) cached;   // 序列化器存JSON时埋了类型信息，取出来就已是WeatherVo，直接强转
        }
        try {
            // ② 造带Key的"信封"：和风要求Key放请求头X-QW-Api-Key（头不进访问日志，比URL安全）
            //    三次调用共用同一个信封，造一次就够
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-QW-Api-Key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // ③ 第一跳：城市名→城市ID（和风的天气接口只认ID不认名字）
            //    {c}占位符由末尾的city参数填充，Spring顺便把中文转成URL合法的%XX编码
            String lookupJson;
            try {
                lookupJson = restTemplate.exchange(
                        apiHost + "/geo/v2/city/lookup?location={c}",
                        HttpMethod.GET, entity, String.class, city).getBody();
            } catch (HttpClientErrorException e) {
                // RestTemplate遇到HTTP 4xx不返回body而是抛这个异常。
                // 和风"查无此城"= HTTP 400（实测抓包确认）——用户参数的锅，转422；
                // 其他4xx（如401 Key错）原样抛出去，由外层兜底转502
                if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 404) {
                    throw new BusinessException(422, "查不到这个城市");
                }
                throw e;
            }
            JsonNode loc = readOk(lookupJson).path("location").path(0);   // 进location数组取第0个
            if (loc.isMissingNode()) {
                // 双保险：HTTP是200但数组是空的（理论上不该发生，防御一手）
                throw new BusinessException(422, "查不到这个城市");
            }
            String id = loc.path("id").asText();          // 形如"101210101"，后两跳都用它

            // ④ 第二跳：实时天气（now对象里有6个我们要的字段）
            String nowJson = restTemplate.exchange(
                    apiHost + "/v7/weather/now?location={id}",
                    HttpMethod.GET, entity, String.class, id).getBody();
            JsonNode now = readOk(nowJson).path("now");

            // ⑤ 第三跳：3天预报——只为拿今天(daily[0])的最高/最低温
            String dailyJson = restTemplate.exchange(
                    apiHost + "/v7/weather/3d?location={id}",
                    HttpMethod.GET, entity, String.class, id).getBody();
            JsonNode today = readOk(dailyJson).path("daily").path(0);

            // ⑥ 翻译：三坨和风JSON → 我们契约的10个字段（对照文档附录的翻译表逐行搬）
            WeatherVo vo = new WeatherVo();
            vo.setCity(loc.path("name").asText());        // 用和风解析后的规范名回显，不用用户的原始输入
            vo.setTemp(now.path("temp").asInt());         // 和风数字都是字符串"33"，asInt()连脱引号带转型
            vo.setText(now.path("text").asText());
            vo.setIcon(now.path("icon").asText());
            vo.setTempMax(today.path("tempMax").asInt());
            vo.setTempMin(today.path("tempMin").asInt());
            vo.setHumidity(now.path("humidity").asInt());
            vo.setWindDir(now.path("windDir").asText());
            vo.setWindScale(now.path("windScale").asText());
            // obsTime原文"2026-07-17T10:35+08:00"：截前16位去掉时区尾巴，再把T换成空格
            vo.setUpdateTime(now.path("obsTime").asText().substring(0, 16).replace("T", " "));
            redisTemplate.opsForValue().set(weatherRedis,vo, Duration.ofMinutes(30));
            return vo;


        } catch (BusinessException e) {
            throw e;   // 自己抛的422/502原样放行，别被下面的兜底误伤改判
        } catch (Exception e) {
            // 超时、连不上、JSON解析失败……一切意外统一转502——第三方的锅不能以500的样子冒出来
            // 转译前必须先把原始异常打进日志：给前端的是体面话术，给自己的是真相。
            // （gzip乱码那次就是没这行，排查时两眼一抹黑）
            log.error("调用和风天气失败", e);
            throw new BusinessException(502, "天气服务暂时不可用");
        }


    }

    /**
     * 小工具：解析JSON + 判和风自己的业务code（拆成两个方法是因为lookup那跳
     * 要在checkCode之前先拦404，需要单独拿到解析后的树）。
     * 和风是"HTTP签收了不代表业务成功"的流派：Key错、超额时HTTP照样通，真相在body的code里，
     * 所以每一跳取数据前都要先过这道闸——三跳共用，同一口径只写一份（老原则）。
     */
    private JsonNode readOk(String json) throws Exception {
        return checkCode(objectMapper.readTree(json));
    }

    private JsonNode checkCode(JsonNode root) {
        if (!"200".equals(root.path("code").asText())) {   // 注意和风的code是字符串"200"
            throw new BusinessException(502, "天气服务暂时不可用");
        }
        return root;
    }
}
