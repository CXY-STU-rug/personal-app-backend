package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表盘 todo.list 的元素（契约3.21：只给4个字段，remark/status都不要——
 * list里全是未完成的，status必然是0，传了也是废字节）
 */
@Data
public class TodoBriefVo {

    private Long id;                 // 任务id，前端点击跳转用

    private String title;            // 标题

    private Integer priority;        // 1=高 2=中 3=低，前端画角标用

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;  // 截止时间
}
