package com.liyuq.service;

import com.liyuq.VO.CalendarVo;
import com.liyuq.VO.DayViewVo;

import java.util.List;

/**
 * 日历查询服务（契约3.22/3.23）。
 * 纯聚合服务：没有自己的表，只是把schedules和todos两张表的数据按日历口径组装——
 * 所以是普通接口，不继承IService（IService是"一个service管一张表"时用的，和DashboardService同理）。
 */
public interface CalendarService {

    List<CalendarVo> monthView(String month);   // 3.22 月视图打点：month形如"2026-07"

    DayViewVo dayView(String date);             // 3.23 日视图明细：date形如"2026-07-16"
}
