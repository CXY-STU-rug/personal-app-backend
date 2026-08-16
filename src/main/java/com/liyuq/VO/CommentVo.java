package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

// 3.40 评论列表返回的单条评论结构（对齐文档 JSON：id/userId/userName/content/createdAt）
@Data
public class CommentVo {

    private Long id;          // 评论ID

    private Long userId;      // 评论人ID

    private String userName;  // 评论人昵称（要 join/批量查 users 表得到）

    private String content;   // 评论内容

    // 时间格式化，否则 LocalDateTime 会被序列化成带 T 的 ISO 串
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
