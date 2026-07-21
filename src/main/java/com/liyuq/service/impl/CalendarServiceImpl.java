package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyuq.VO.CalendarVo;
import com.liyuq.VO.DayViewVo;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.VO.TodosVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Schedules;
import com.liyuq.entity.Todos;
import com.liyuq.mapper.SchedulesMapper;
import com.liyuq.mapper.TodosMapper;
import com.liyuq.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 日历查询实现（契约3.22/3.23）：不继承ServiceImpl（没有自己的表），
 * 注入两个mapper把日程和待办按"日历"这个视角重新组装。
 */
@Service
public class CalendarServiceImpl implements CalendarService {
@Autowired
private SchedulesMapper schedulesMapper;
@Autowired
private TodosMapper todosMapper;


    @Override
    public List<CalendarVo> monthView(String month) {
Long userId=UserContext.getUserContextId();

        YearMonth yearMonth = null;
try {yearMonth= YearMonth.parse(month);
}catch (DateTimeParseException e){
    // 422=参数不合法（403是"无权"）；提示语用人话，别把e.getMessage()的英文细节漏给前端
    throw  new BusinessException(422, "month格式应为YYYY-MM");
}

        LocalDateTime start = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(yearMonth.plusMonths(1).atDay(1), LocalTime.MIN);

        List<CalendarVo> timeList = schedulesMapper.selectCount(userId,start,end);
   List<CalendarVo>calendarVos=todosMapper.countTodoByDay(userId,start,end);
        Map<LocalDate, CalendarVo> merged = new TreeMap<>();
//   ↑键=日期        ↑值=拼装中的一行
// 第1步：先把日程打点全部铺进Map——每行转成完整的CalendarVo，缺的todoCount先垫0
        for (CalendarVo s : timeList) {
            CalendarVo vo = new CalendarVo();
            vo.setDate(s.getDate());
            vo.setTodoCount(0);                        // 垫0不留null：契约要求没内容显式给0，前端要拿它做算术
            vo.setScheduleCount(s.getScheduleCount());
            merged.put(s.getDate(), vo);
        }
        // 第2步：叠加待办打点——两种情况：这天Map里已经有行了 / 还没有
        for (CalendarVo t : calendarVos) {
            CalendarVo vo = merged.get(t.getDate());   // 拿日期去Map里找已有的行
            if (vo == null) {                          // 这天只有待办没日程 → 新建一行
                vo = new CalendarVo();
                vo.setScheduleCount(0);
                vo.setDate(t.getDate());
                merged.put(t.getDate(), vo);
            }
            vo.setTodoCount(t.getTodoCount());         // 不管哪种情况，最后把待办数设进vo
        }

// 第3步：Map的值倒成List返回（TreeMap保证了日期升序）
        return new ArrayList<>(merged.values());

    }

    @Override
    public DayViewVo dayView(String date) {
        Long userId = UserContext.getUserContextId();   // 老规矩：身份只从token取

        // ① 解析"2026-07-16"→[当天0点, 次日0点)的半开区间——和月视图框月份是同一个思路，只是缩小到一天
        LocalDate day;
        try {
            day = LocalDate.parse(date);                // 格式不对会抛异常
        } catch (DateTimeParseException e) {
            throw new BusinessException(422, "date格式应为YYYY-MM-DD");
        }
        LocalDateTime begin = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // ② 这天的日程：start_time落在当天（日程不跨天，按开始时间归属），按开始时间升序=一天的时间轴
        LambdaQueryWrapper<Schedules> sw = new LambdaQueryWrapper<>();
        sw.eq(Schedules::getUserId, userId)             // 数据隔离
          .ge(Schedules::getStartTime, begin)           // >= 当天0点
          .lt(Schedules::getStartTime, end)             // <  次日0点（半开区间）
          .orderByAsc(Schedules::getStartTime);         // 契约3.23：startTime升序
        List<ScheduleVo> schedules = new ArrayList<>();
        for (Schedules s : schedulesMapper.selectList(sw)) {   // 实体→Vo，只搬契约要的5个字段
            ScheduleVo vo = new ScheduleVo();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setRemark(s.getRemark());                // 日视图要展示备注（冲突列表不用）
            vo.setStartTime(s.getStartTime());
            vo.setEndTime(s.getEndTime());
            schedules.add(vo);
        }

        // ③ 这天截止的待办：deadline落在当天，按截止时间升序；不过滤status——回顾要看到已完成的
        LambdaQueryWrapper<Todos> tw = new LambdaQueryWrapper<>();
        tw.eq(Todos::getUserId, userId)
          .ge(Todos::getDeadline, begin)
          .lt(Todos::getDeadline, end)
          .orderByAsc(Todos::getDeadline);              // 契约3.23：deadline升序
        List<TodosVo> todos = new ArrayList<>();
        for (Todos t : todosMapper.selectList(tw)) {
            TodosVo vo = new TodosVo();
            vo.setId(t.getId());
            vo.setTitle(t.getTitle());
            vo.setRemark(t.getRemark());
            vo.setPriority(t.getPriority());
            vo.setStatus(t.getStatus());                // 前端靠它给已完成的画删除线
            vo.setDeadline(t.getDeadline());
            todos.add(vo);
        }

        // ④ 两个列表装进返回体——字段名schedules/todos是前端的取值路径
        DayViewVo vo = new DayViewVo();
        vo.setSchedules(schedules);
        vo.setTodos(todos);
        return vo;
    }
}
