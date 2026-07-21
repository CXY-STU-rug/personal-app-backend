package com.liyuq.VO;


import lombok.Data;

import java.util.List;

@Data                              // 列表/搜索的外层壳：VO套VO
public class TotalnumbersnotesVo {
   private Long total;             // 总条数(分页时=总数；搜索时=结果数)
   private List<notesVo> list;     // 当前这批笔记，每个是一条 notesVo

}
