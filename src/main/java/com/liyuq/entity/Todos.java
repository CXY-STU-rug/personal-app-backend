package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("todos")
public class Todos {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    private Integer priority;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
