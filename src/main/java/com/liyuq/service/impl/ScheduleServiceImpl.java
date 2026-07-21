package com.liyuq.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyuq.DTO.ScheduleDto;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.Exception.ConflictException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Schedules;
import com.liyuq.mapper.SchedulesMapper;
import com.liyuq.service.SchedulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class ScheduleServiceImpl extends ServiceImpl<SchedulesMapper,Schedules> implements SchedulesService {

    @Autowired
    private SchedulesMapper schedulesMapper;


    @Override
    public ScheduleVo createschedule(ScheduleDto dto) {
        Long userId=UserContext.getUserContextId();   // 身份只从token取，防越权

        LocalDateTime cTime=dto.getStartTime();
        LocalDateTime eTime=dto.getEndTime();
        if(!eTime.isAfter(cTime)){
            // "不晚于"=早于或等于：连"结束=开始"的零长度日程一起拦住；422=参数本身不合法
            throw new BusinessException(422,"结束时间必须晚于开始时间");
        }
        if(!cTime.toLocalDate().equals(eTime.toLocalDate())){
            // toLocalDate()砍掉时分秒只留日期，两边不相等就是跨天——V2不做跨天日程（契约3.24）
            throw new BusinessException(422,"日程不能跨天");
        }

        if (!dto.isForce()) {   // force=true：用户已确认"仍要保存"，跳过检测（连查询也一起省了）
            List<ScheduleVo> conflicts = findConflicts(userId, cTime, eTime, null);   // create没有"自己"，excludeId传null
            if (!conflicts.isEmpty()) {
                // 409=参数合法但和现状打架；冲突列表随异常带出去，Handler装进响应的data
                throw new ConflictException(409, "该时间段已有安排", conflicts);
            }
        }

        Schedules schedule=new Schedules();
        schedule.setUserId(userId);
        schedule.setStartTime(cTime);                  // 日程时间来自dto
        schedule.setEndTime(eTime);
        schedule.setTitle(dto.getTitle());
        schedule.setRemark(dto.getRemark());
        schedule.setCreatedAt(LocalDateTime.now());    // 审计时间戳=此刻；created_at列NOT NULL，不填插入报错
        schedule.setUpdatedAt(LocalDateTime.now());
        schedulesMapper.insert(schedule);
        ScheduleVo schedulesVo=new ScheduleVo();
        schedulesVo.setId(schedule.getId());           // insert后MyBatis-Plus会把自增id回填进实体
        schedulesVo.setTitle(dto.getTitle());
        schedulesVo.setStartTime(dto.getStartTime());
        schedulesVo.setEndTime(dto.getEndTime());
        return schedulesVo;

    }

    @Override
    public Integer updateSchedule(Long id, ScheduleDto dto) {
        Long userId=UserContext.getUserContextId();
        Schedules old = GetScheduleOwner(id);          // 归属校验，顺便拿到旧数据——"没传用旧值"要靠它

        // 局部更新口径："新值优先，没传保持旧值"——只改标题时dto里时间是null，拿null去校验会NPE
        LocalDateTime cTime = dto.getStartTime() != null ? dto.getStartTime() : old.getStartTime();
        LocalDateTime eTime = dto.getEndTime()   != null ? dto.getEndTime()   : old.getEndTime();

        // 对"最终时段"做和create同一套校验
        if(!eTime.isAfter(cTime)){
            throw new BusinessException(422,"结束时间必须晚于开始时间");
        }
        if(!cTime.toLocalDate().equals(eTime.toLocalDate())){
            throw new BusinessException(422,"日程不能跨天");
        }

        if (!dto.isForce()) {
            // 和create唯一的区别：excludeId传自己的id——不排除的话，新时段和改之前的自己相撞，永远改不动（契约3.25的经典坑）
            List<ScheduleVo> conflicts = findConflicts(userId, cTime, eTime, id);
            if (!conflicts.isEmpty()) {
                throw new ConflictException(409, "该时间段已有安排", conflicts);
            }
        }

        if (dto.getTitle() != null)  old.setTitle(dto.getTitle());    // 传了才改，null=保持原值
        if (dto.getRemark() != null) old.setRemark(dto.getRemark());
        old.setStartTime(cTime);                       // 最终时段（可能是新值也可能保持原值）
        old.setEndTime(eTime);
        old.setUpdatedAt(LocalDateTime.now());         // 更新时间无条件刷新
        schedulesMapper.updateById(old);
        return 1;                                      // 契约3.25：成功返回 data: 1
    }

    @Override
    public void deleteSchedule(Long id) {
        GetScheduleOwner(id);        // 只借它做"存在+归属"校验
        schedulesMapper.deleteById(id);

    }

    /**
     * 找出和[start, end)重叠的本人日程并转成Vo——create/update共用的唯一一份冲突口径（文档§5.2）。
     * excludeId：update传自己的id排除自己，create传null。
     */
    private List<ScheduleVo> findConflicts(Long userId, LocalDateTime start,
                                           LocalDateTime end, Long excludeId) {
        // 公式直接写进SQL：已有.start < 新.end AND 已有.end > 新.start
        // lt/gt都是严格比较（半开区间，首尾相接不算冲突）；链式条件之间默认就是AND
        LambdaQueryWrapper<Schedules> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedules::getUserId, userId)       // 只查自己的，数据隔离
               .lt(Schedules::getStartTime, end)       // 已有.start < 新.end
               .gt(Schedules::getEndTime, start);      // 已有.end   > 新.start
        if (excludeId != null) {
            wrapper.ne(Schedules::getId, excludeId);   // 排除自己
        }
        // 数据库只返回撞车的，循环只负责实体→Vo（装已有日程的时间，告诉前端"你和谁撞了"）
        List<ScheduleVo> conflicts = new ArrayList<>();
        for (Schedules s : schedulesMapper.selectList(wrapper)) {
            ScheduleVo vo = new ScheduleVo();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setStartTime(s.getStartTime());
            vo.setEndTime(s.getEndTime());
            conflicts.add(vo);
        }
        return conflicts;
    }

    private Schedules GetScheduleOwner(Long id) {
        Long userId=UserContext.getUserContextId();

        Schedules schedules=schedulesMapper.selectById(id);
        if (schedules == null) {
            // 先判存在再判归属：查不到是404"资源不存在"，不是403"无权"
            throw new BusinessException(404,"日程不存在");
        }
        if(!schedules.getUserId().equals(userId)){     // Long要用equals比数值，==/!=比的是引用
            throw new BusinessException(403,"无权操作");
        }
        return schedules;

    }

}
