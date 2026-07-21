package com.liyuq.DTO;


import lombok.Data;

import java.util.List;

@Data                              // 3.15 更新笔记的入参容器，对应请求体JSON
public class UpdateNoteDto {
    private String title;          // 新标题

    private String content;        // 新正文

   private List<String> tagNames;  // 新标签名数组，全量覆盖(传什么这条笔记就只剩什么)
   // 注意：不放 userId——归属由token决定，绝不能让前端传，防越权


}
