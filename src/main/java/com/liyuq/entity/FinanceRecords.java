package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;      // 算钱专用类型，绝不用double（会丢小数精度）
import java.time.LocalDateTime;   // 带日期+时间的类型，对应数据库DATETIME

@Data
@TableName("finance_records")     // 对应 finance_records 记账记录表
public class FinanceRecords {
    @TableId(type = IdType.AUTO)  // 主键自增，insert后回填
    private Long id;              // 表里BIGINT→Long（记录可能很多，用大整数）
    private Long userId;          // 属主用户id，数据隔离的关键：查/改/删都要用它锁归属
    private Integer categoryId;   // 所属分类id，INT→Integer，指向finance_categories.id
    private Integer type;         // 1=支出 2=收入，冗余存一份便于统计，创建时校验必须和分类的type一致
    private BigDecimal amount;    // 金额，DECIMAL(10,2)→BigDecimal
    private String remark;        // 备注
    private LocalDateTime recordTime;   // 记账发生时间（可以补记过去的账）
    private LocalDateTime createdAt;    // 进库时间（出生证明，只在创建时写一次，update永不碰）
}
