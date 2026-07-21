package com.liyuq.DTO;

import lombok.Data;

import java.util.List;

@Data                              // 3.14 新建笔记的入参容器，对应前端发的JSON body
public class CreateNoteDto {
    private String title;          // 标题
    private String content;        // 正文
    private List<String> tagNames; // 标签名数组——字段名必须叫 tagNames，跟前端对齐（原来叫list对不上）
    // 同样不放 userId：归属由token决定，防越权
}
