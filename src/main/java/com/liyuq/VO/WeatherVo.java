package com.liyuq.VO;

import lombok.Data;

/**
 * 实时天气返回体（接口文档3.27的10个字段）
 * 这个VO的特殊使命："翻译层"——第三方（和风）返回什么字段名前端不用管，
 * 前端只认识这套契约；以后换高德/换服务商，改的是service里的翻译代码，这个VO和前端一行不动
 */
@Data
public class WeatherVo {

    private String city;        // 城市名（后端解析后的规范名，回显用）

    private Integer temp;       // 当前气温（℃）——第三方给的是字符串"33"，翻译时要转成int，契约承诺是数字

    private String text;        // 天气文字：晴/多云/小雨…（前端主要靠它选图标）

    private String icon;        // 第三方的图标代码，前端可用可不用，原样透传

    private Integer tempMax;    // 今天最高温

    private Integer tempMin;    // 今天最低温

    private Integer humidity;   // 相对湿度（%）

    private String windDir;     // 风向，如"东南风"

    private String windScale;   // 风力等级，如"3"——契约定的是string不是int（第三方可能给"3-4"这种区间）

    private String updateTime;  // 数据观测时间"2026-07-17 10:35"——注意精确到分、没有秒，
                                // 且它来自第三方原文只做透传、不参与任何计算，所以直接用String，
                                // 不必像deadline那样转LocalDateTime再@JsonFormat转回来（转两道纯属绕路）
}
