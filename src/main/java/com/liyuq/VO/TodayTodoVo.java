package com.liyuq.VO;

import lombok.Data;

import java.util.List;

/**
 * 仪表盘的 todo 块（契约3.21）。
 * 关键设计：计数和列表是两个独立的东西——
 * list只装未完成前3条，已完成的信息在list里根本不存在，
 * 所以进度条要的 todayTotal/todayDone 必须单独给，前端没法从list自己算。
 */
@Data
public class TodayTodoVo {

    private Integer todayTotal;       // 今天截止的任务总数（含已完成）

    private Integer todayDone;        // 其中已完成的条数（status=1）

    private List<TodoBriefVo> list;   // 今天未完成的前3条，priority升序（高优先级在前）
}
