package com.liyuq.VO;

import lombok.Data;

@Data                         // Lombok：自动生成 getter/setter，省得手写
public class NoteTagsVo {
    private Integer id;       // 标签id，跟实体 NoteTags.id 对齐（自增，Integer）
    private String name;      // 标签名，前端下拉框显示用
    // 注意：实体 NoteTags 里还有 userId，这里故意不放——不能把归属信息漏给前端
}
