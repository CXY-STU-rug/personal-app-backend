package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liyuq.VO.DashboardVo;
import com.liyuq.VO.FinanceBriefVo;
import com.liyuq.VO.FinanceStatisticsVo;
import com.liyuq.VO.NoteBriefVo;
import com.liyuq.VO.TodayTodoVo;
import com.liyuq.VO.TodoBriefVo;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Notes;
import com.liyuq.entity.Todos;
import com.liyuq.service.DashboardService;
import com.liyuq.service.FinanceRecordsService;
import com.liyuq.service.NotesService;
import com.liyuq.service.TodosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 仪表盘实现（契约3.21）：跨模块聚合，自己不写统计SQL，复用各模块service。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private TodosService todosService;                    // 除了自定义方法，还白送IService的 list(Wrapper)
    @Autowired
    private FinanceRecordsService financeRecordsService;  // 复用 3.12 的统计方法
    @Autowired
    private NotesService notesService;                    // 同样白送 list(Wrapper)

    @Override
    public DashboardVo getOverview() {
        DashboardVo vo = new DashboardVo();
        Long userId = UserContext.getUserContextId();   // 老规矩：身份只从token取，防越权

        // ═══════ ① todo 块：先数后筛 ═══════
        // 第1步：查"今天截止"的全部任务——注意这里不过滤status（含已完成），
        // 因为 todayTotal/todayDone 两个计数都要基于"全部"来算
        LocalDateTime start = LocalDate.now().atStartOfDay();          // 今天 00:00:00
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);     // 今天 23:59:59.999
        QueryWrapper<Todos> todoWrapper = new QueryWrapper<>();
        todoWrapper.eq("user_id", userId)                              // 数据隔离
                   .between("deadline", start, end);                   // deadline落在今天范围内
        List<Todos> todayAll = todosService.list(todoWrapper);         // IService白送的按条件查列表

        // 第2步：先数——必须在过滤之前算，一旦先把已完成的filter掉，todayDone就永远是0了
        int todayTotal = todayAll.size();                              // 今天的总数（含已完成）
        int todayDone = 0;
        for (Todos t : todayAll) {                                     // 数一数其中已完成的
            if (Integer.valueOf(1).equals(t.getStatus())) {            // Integer用equals比，顺带防null
                todayDone++;
            }
        }

        // 第3步：再筛——未完成 → priority升序(1高在前) → 最多3条 → 转成契约要的小VO
        // 这是"一次查询、多种加工"：计数和列表用的是同一批数据，数据库只跑了一次
        List<TodoBriefVo> todoList = todayAll.stream()
                .filter(t -> Integer.valueOf(0).equals(t.getStatus())) // 只留未完成的
                .sorted(Comparator.comparingInt(Todos::getPriority))   // 按priority从小到大排
                .limit(3)                                              // 截断：最多取3条
                .map(t -> {                                            // 实体→TodoBriefVo，只搬4个契约字段
                    TodoBriefVo brief = new TodoBriefVo();
                    brief.setId(t.getId());
                    brief.setTitle(t.getTitle());
                    brief.setPriority(t.getPriority());
                    brief.setDeadline(t.getDeadline());
                    return brief;
                })
                .toList();                                             // 收集成List

        // 第4步：三样装进todo块，挂到返回体上
        TodayTodoVo todo = new TodayTodoVo();
        todo.setTodayTotal(todayTotal);
        todo.setTodayDone(todayDone);
        todo.setList(todoList);
        vo.setTodo(todo);

        // ═══════ ② finance 块：复用3.12后"做减法" ═══════
        // 复用统计service（同一个口径只能有一份实现），再只挑首页要的3个字段搬进精简VO
        String month = YearMonth.now().toString();   // "2026-07"，正好是3.12要的格式
        FinanceStatisticsVo stat = financeRecordsService.listStatisticFinanceRecords(month);
        FinanceBriefVo finance = new FinanceBriefVo();
        finance.setTotalIncome(stat.getTotalIncome());     // 三个总额照搬
        finance.setTotalExpense(stat.getTotalExpense());
        finance.setBalance(stat.getBalance());
        // categoryBreakdown故意不搬——首页不画占比图，契约也不要
        vo.setFinance(finance);

        // ═══════ ③ notes 块：按updated_at倒序取3条 ═══════
        // 注意口径：契约要"最近更新"=按updated_at倒序，不是id倒序——
        // 一篇老笔记刚被编辑过就该排最前面，按id排它永远沉底，所以不能复用queryNotes
        QueryWrapper<Notes> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId)               // 数据隔离，一如既往
                   .orderByDesc("updated_at")           // 最近更新的在前
                   .last("LIMIT 3");                    // 只要3条：last()把这段原样拼到SQL末尾
        List<Notes> noteList = notesService.list(noteWrapper);

        List<NoteBriefVo> notes = new ArrayList<>();
        for (Notes n : noteList) {                      // 实体→精简VO，只搬契约要的3个字段（不带tags，省掉查关联表）
            NoteBriefVo brief = new NoteBriefVo();
            brief.setId(n.getId());
            brief.setTitle(n.getTitle());
            brief.setUpdatedAt(n.getUpdatedAt());
            notes.add(brief);
        }
        vo.setNotes(notes);

        return vo;
    }
}
