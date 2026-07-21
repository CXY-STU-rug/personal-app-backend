package com.liyuq.DTO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新待办的请求体（接口文档3.5）：全部选填，null=不修改该字段
 * 比创建DTO多一个status——勾选完成/取消完成走的就是这个字段
 */
@Data
public class UpdateTodosDto {

    private String title;      // 选填，null则不修改

    private String remark;     // 备注（选填）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输入方向：把"2026-07-15 18:00:00"解析成LocalDateTime
    private LocalDateTime deadline;                // 截止时间（选填）

    private Integer priority;  // 优先级1/2/3（选填，不传靠数据库DEFAULT 2）

    private Integer status;

}
