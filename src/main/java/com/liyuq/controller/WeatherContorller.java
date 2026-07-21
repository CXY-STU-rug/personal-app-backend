package com.liyuq.controller;


import com.liyuq.VO.WeatherVo;
import com.liyuq.common.Result;
import com.liyuq.service.WeatherService;
import com.liyuq.service.impl.WeatherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherContorller {


    @Autowired
    private WeatherService weatherService;

    @GetMapping("/now")
    public Result<WeatherVo> getNow(@RequestParam(required = false)String city)
    {
        return Result.success(weatherService.getWeatherNow(city));   // service的返回值装进统一信封交给前端
    }

}
