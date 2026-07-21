package com.liyuq.DTO;


import lombok.Data;

@Data                          // 3.13 列表查询的入参，Spring按字段名从query参数填充
public class queryNotesDto {

    private Integer tagId;     // 可选：按标签筛选(预留，当前列表主查未用)

    private Integer page;      // 第几页

    private Integer pageSize;  // 每页几条
}
