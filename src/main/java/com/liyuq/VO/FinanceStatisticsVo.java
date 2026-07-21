package com.liyuq.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 3.12 月度收支统计的外层返回：三个总额 + 分类明细数组。
 * VO套VO的写法：categoryBreakdown 字段类型是 List<内层VO>，
 * Jackson序列化时自然就长成文档里 data 套数组的样子。
 */
@Data
public class FinanceStatisticsVo {

    private BigDecimal totalIncome;    // 本月总收入（type=2的合计）

    private BigDecimal totalExpense;   // 本月总支出（type=1的合计）

    private BigDecimal balance;        // 结余 = totalIncome - totalExpense（用subtract算）

    private List<CategoryBreakdownVo> categoryBreakdown;   // 每个分类一条的明细，嵌套点在这
}
