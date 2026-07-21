package com.liyuq.DTO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建待办的请求体（接口文档3.4的Body参数直译）
 * 字段白名单：故意没有userId/id/status/createdAt——"后端自己知道的"绝不让前端传
 */
@Data
public class TodosDto {

private String title;      // 任务标题（文档：必选，1-100位）

    private String remark;     // 备注（选填）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输入方向：把"2026-07-15 18:00:00"解析成LocalDateTime
    private LocalDateTime deadline;                // 截止时间（选填）

    private Integer priority;  // 优先级1/2/3（选填，不传靠数据库DEFAULT 2）

}
