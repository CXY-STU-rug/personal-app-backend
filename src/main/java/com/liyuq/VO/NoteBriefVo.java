package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表盘的 notes 元素（契约3.21）：只给 id/title/updatedAt。
 * 不带tags——省掉了每篇笔记查关联表拼标签的开销，首页也不展示标签。
 */
@Data
public class NoteBriefVo {

    private Long id;                  // 笔记id，点击跳详情用

    private String title;             // 标题

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;  // 更新时间，首页显示"最近更新"
}
