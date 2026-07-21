package com.liyuq.VO;

import lombok.Data;

/**
 * AI执行的一个写操作（接口文档3.28里actions数组的元素）
 * 前端靠它弹提示"AI替你改了数据"；只记写操作，查询不算
 */
@Data
public class AiActionVo {

    private String type;     // 操作类型枚举：todo_create / finance_create（契约只定了这两种写操作）

    private String detail;   // 人话描述，如"餐饮 支出 200.00"，前端直接展示
}
