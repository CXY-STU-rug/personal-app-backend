package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.VO.CalendarVo;
import com.liyuq.entity.Schedules;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SchedulesMapper extends BaseMapper<Schedules> {

@Select("select DATE(start_time) As date,count(*) As scheduleCount from schedules where user_id=#{userId} and start_time>=#{start} and start_time<#{end} GROUP BY DATE(start_time)")
    List<CalendarVo> selectCount(Long userId, LocalDateTime start, LocalDateTime end);
}
