package com.liyuq.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedules")   // 显式指定表名，和其他实体保持一致
public class Schedules {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;      // 所属用户，数据隔离用

    private String title;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 中间是空格不是冒号
    private LocalDateTime startTime;               // 日程几点开始（业务时间，冲突检测的主角）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;                 // 日程几点结束

    private LocalDateTime createdAt;               // 记录几点插进库（审计时间戳），对应列 created_at
    private LocalDateTime updatedAt;               // 记录最后修改时间，对应列 updated_at
}
