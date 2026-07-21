package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;      // 主键生成策略的枚举
import com.baomidou.mybatisplus.annotation.TableId;     // 标注哪个字段是主键
import com.baomidou.mybatisplus.annotation.TableName;   // 标注这个类对应哪张表
import lombok.Data;

@Data                                  // Lombok生成getter/setter等
@TableName("finance_categories")       // 类↔表的映射：这个实体对应 finance_categories 表
public class FinanceCategories {
    @TableId(type = IdType.AUTO)       // 主键，AUTO=用数据库自增值，insert后MP会把生成的id回填进对象
    private Integer id;   // 表里是INT，和records.categoryId那条Integer链保持同一类型（跨类型equals永远false）
    private String name;  // 分类名
    private Integer type; // 1=支出 2=收入
    private String icon;  // 图标标识
}
