package com.liyuq.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账记录的"进门登记表"（接口文档3.9的Body参数表直译，3.10更新也共用它）
 * 字段白名单：故意没有userId/id/createdAt——后端自己知道的绝不让前端传，
 * 前端就算多传了这些键，Jackson因为DTO里没有对应字段会直接丢弃
 */
@Data
public class FinanceRecordsDto {



    private Integer categoryId;   // 分类ID（创建必选），service要拿它去分类表做跨表校验

    private Integer type;         // 1=支出 2=收入（创建必选），必须和categoryId对应分类的type一致

    private BigDecimal amount;    // 金额（创建必选，>0），文档decimal→BigDecimal，绝不用double（小数精度会丢）

    private String remark;        // 备注（选填，最长200位）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输入方向：把"2026-07-14 12:30:00"解析成LocalDateTime
    private LocalDateTime recordTime;              // 记账发生时间（创建必选），允许补记过去的账；和createdAt（进库时间）是两回事

}
