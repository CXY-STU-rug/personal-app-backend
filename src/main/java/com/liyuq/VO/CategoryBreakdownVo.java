package com.liyuq.VO;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 3.12 统计接口里 categoryBreakdown 数组的每一项：某个分类这个月花/赚了多少、占比多少。
 * 它还兼职当手写SQL的接收容器：SQL只查得出前三个字段(categoryId/type/amount)，
 * categoryName 和 percentage 查完时是 null，由 service 在 Java 里补上。
 */
@Data
public class CategoryBreakdownVo {

    private Integer categoryId;      // 分类id（和records.categoryId同一条Integer类型链）

    private String categoryName;     // 分类名——SQL查不出，service用nameMap补

    private Integer type;            // 1支出 2收入（决定百分比的分母用哪个总额）

    private BigDecimal amount;       // 该分类本月合计，SQL的SUM(amount)直接落在这

    private BigDecimal percentage;   // 占比——service里除法算出来再塞进去，保留1位小数
}
