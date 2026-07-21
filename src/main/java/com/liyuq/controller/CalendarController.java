package com.liyuq.controller;


import com.liyuq.VO.CalendarVo;
import com.liyuq.VO.DayViewVo;
import com.liyuq.common.Result;
import com.liyuq.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")   // 查询类接口挂calendar，增删改挂schedule——URL按资源命名（文档§5.6）
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    // 3.22 月视图打点：?month=2026-07
    // 参数类型是String不是Long——"2026-07"带横杠，Long接会直接400类型转换失败
    @GetMapping("/month")
    public Result<List<CalendarVo>> month(@RequestParam String month) {
        return Result.success(calendarService.monthView(month));
    }

    // 3.23 日视图明细：?date=2026-07-16
    @GetMapping("/day")
    public Result<DayViewVo> day(@RequestParam String date) {
        return Result.success(calendarService.dayView(date));
    }
}
