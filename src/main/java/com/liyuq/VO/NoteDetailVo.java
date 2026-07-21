package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data                                 // 笔记详情的返回形状：比列表 notesVo 多了 content 全文
public class NoteDetailVo {
    private Long id;                  // 笔记id
    private String title;            // 标题
    private String content;          // 正文全文——详情才给，列表不给(这是列表/详情的关键区别)
    private List<String> tags;// 标签名数组，前端编辑页用(item.tags)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // 更新时间
}
