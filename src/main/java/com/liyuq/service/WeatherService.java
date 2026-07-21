package com.liyuq.service;

import com.liyuq.VO.WeatherVo;

public interface WeatherService {


    WeatherVo getWeatherNow(String city);
}
