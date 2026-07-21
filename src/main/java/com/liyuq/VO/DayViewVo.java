package com.liyuq.VO;

import lombok.Data;

import java.util.List;

/**
 * 日视图返回体（契约3.23）：点日历上某一天，这天的全部日程+待办。
 * 字段名 schedules/todos 是前端的取值路径（data.schedules / data.todos），不能改。
 */
@Data
public class DayViewVo {

    private List<ScheduleVo> schedules;   // 这天的日程，按startTime升序（一天的时间轴）

    private List<TodosVo> todos;          // 这天截止的待办，按deadline升序
}
