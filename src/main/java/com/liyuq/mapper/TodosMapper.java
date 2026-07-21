package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.VO.CalendarVo;
import com.liyuq.entity.Todos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface TodosMapper extends BaseMapper<Todos> {

    // 月视图的待办打点（契约3.22）：deadline落在当天的待办数
    // 注意：不过滤status——含已完成，日历是回顾工具，做完的也要能看到
    @Select("SELECT DATE(deadline) AS date, COUNT(*) AS todoCount FROM todos " +   // 待办的时间列是deadline；start_time是schedules表的
            "WHERE user_id=#{userId} AND deadline >= #{begin} AND deadline < #{end} " +
            "GROUP BY DATE(deadline)")
    List<CalendarVo> countTodoByDay(Long userId, LocalDateTime begin, LocalDateTime end);
}
