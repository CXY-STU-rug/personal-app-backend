package com.liyuq.VO;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表盘的 finance 块（契约3.21）：只要三个总额。
 * 和3.12统计VO的区别：砍掉了categoryBreakdown——首页不画占比图，
 * 传了也没人用。这就是"聚合接口只返回首页真正展示的字段"的设计原则。
 */
@Data
public class FinanceBriefVo {

    private BigDecimal totalIncome;    // 本月总收入

    private BigDecimal totalExpense;   // 本月总支出

    private BigDecimal balance;        // 结余 = 收入 - 支出
}
