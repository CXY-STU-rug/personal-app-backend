package com.liyuq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyuq.DTO.ScheduleDto;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.entity.Schedules;

public interface SchedulesService extends IService<Schedules> {


    ScheduleVo createschedule(ScheduleDto dto);

    Integer updateSchedule(Long id, ScheduleDto dto);   // 契约3.25：成功返回data:1，不是对象

    void deleteSchedule(Long id);
}
