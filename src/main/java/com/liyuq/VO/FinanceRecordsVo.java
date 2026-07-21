package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账记录的"出门的衣服"（接口文档3.8返回示例的7个键，一键一字段）
 * 和实体比：少了userId（不外泄）、createdAt（页面不展示）；
 * 多了categoryName——records表里没有这个列，是service从分类表查出来拼上的。
 * VO不只对实体做减法，也可以做加法，这是第一个例子
 */
@Data
public class FinanceRecordsVo {

    private Long id;              // 记录ID，表里BIGINT对应Long；前端点编辑/删除时要靠它拼URL

    private Integer categoryId;   // 分类ID，编辑页回填时定位选中的分类

    private String categoryName;  // 分类名（如"餐饮"），列表页直接显示，省得前端再查一次分类表

    private Integer type;         // 1=支出 2=收入，前端决定金额显示成红色还是绿色

    private BigDecimal amount;    // 金额，DECIMAL(10,2)对应BigDecimal

    private String remark;        // 备注

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输出方向：按文档承诺的格式序列化，去掉ISO默认的T
    private LocalDateTime recordTime;              // 记账发生时间

}
