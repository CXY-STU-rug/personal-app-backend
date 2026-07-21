package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data                                 // 列表/搜索里"一条笔记"的展示形状
public class notesVo {

    private Long id;                  // 笔记id
    private String title;            // 标题(列表不返回正文content，省流量)
    private List<String> tags;       // 标签名数组，从关联表拼出来(数据库没这字段)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // 更新时间
}
