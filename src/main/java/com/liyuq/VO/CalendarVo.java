package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 月视图打点（契约3.22）：某一天有几条日程、几条待办。
 * 双重身份：既是mapper里GROUP BY SQL的接收容器（一行=一天），又是最终返回给前端的VO——
 * 和3.12的CategoryBreakdownVo是同一个套路。
 */
@Data
public class CalendarVo {
    @JsonFormat(pattern = "yyyy-MM-dd")   // 大写MM=月份（小写mm是分钟，会渲染成2026-00-16）
    private LocalDate date;               // 字段名小写开头：Java规范 + 前端按 data[i].date 取值

    private Integer scheduleCount;        // 当天开始的日程数（日程不跨天，按start_time归属）

    private Integer todoCount;            // deadline落在当天的待办数，含已完成（日历是回顾工具）
}
