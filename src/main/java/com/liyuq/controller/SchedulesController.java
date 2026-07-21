package com.liyuq.controller;


import com.liyuq.DTO.ScheduleDto;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.common.Result;
import com.liyuq.service.SchedulesService;
import org.springframework.beans.factory.annotation.Autowired;   // @Autowired不在web包里，要单独导
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class SchedulesController {
    @Autowired                       // 不加这个Spring不会注入，字段是null，一调就NPE
    private SchedulesService schedulesService;

    @PostMapping("/create")
    public Result< ScheduleVo> create(@RequestBody ScheduleDto dto) {   // @RequestBody：把JSON请求体绑定到dto，不加则所有字段null
    ScheduleVo scheduleVo= schedulesService.createschedule(dto);

      return Result.success(scheduleVo);
    }
    @PutMapping("/update/{id}")   // 原来多了一个大括号，路径会变成字面量"{id}}"匹配不上
    public Result<Integer> update(@PathVariable Long id,@RequestBody ScheduleDto dto) {
        // 把service的返回值接住递给前端——原来return null，活干了但响应体是空的
        return Result.success(schedulesService.updateSchedule(id,dto));
    }
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        schedulesService.deleteSchedule(id);
        return Result.success();   // 契约3.26：成功data为null，但code/msg的"回执"必须开
    }



}
