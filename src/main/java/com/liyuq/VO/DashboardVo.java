package com.liyuq.VO;

import lombok.Data;

import java.util.List;

/**
 * 首页仪表盘返回体（契约3.21）。
 * 字段名就是前后端的"接头暗号"：前端按 data.todo.todayTotal 这样的路径取值，
 * 所以这三个字段必须叫 todo/finance/notes，差一个字母前端拿到的就是 undefined。
 */
@Data
public class DashboardVo {

    private TodayTodoVo todo;           // 今日待办概况：计数 + 未完成前3条

    private FinanceBriefVo finance;     // 本月收支：只要3个总额

    private List<NoteBriefVo> notes;    // 最近更新的3篇笔记（按updated_at倒序）
}
