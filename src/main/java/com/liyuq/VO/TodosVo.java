package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 待办列表返回体（接口文档3.3的6个字段）
 * 为什么不直接返回实体：实体里有userId/createdAt/updatedAt，页面不展示的不外传
 */
@Data
public class TodosVo {

    private Long id;           // BIGINT对应Long，和实体保持一致

    private String title;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输出方向：去掉ISO格式中间的T，按文档承诺的格式返回
    private LocalDateTime deadline;

    private Integer priority;  // 1=高 2=中 3=低

    private Integer status;    // 0=未完成 1=已完成


}
