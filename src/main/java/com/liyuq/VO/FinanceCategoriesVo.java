package com.liyuq.VO;


import lombok.Data;   // Lombok自动生成getter/setter/toString等，省得手写

/**
 * 记账分类的"出门衣服"（接口文档3.7返回的4个字段）。
 * 这里和实体FinanceCategories字段完全一样，本可以直接返回实体、省掉这个VO——
 * 之所以还留着，是给"以后分类要加字段但不想全暴露给前端"留个隔离层。
 */
@Data
public class FinanceCategoriesVo {
private Integer id;      // 分类ID，和实体一样是Integer（记账记录的categoryId那条Integer链的源头）
private String name;     // 分类名，如"餐饮""工资"
private Integer type;    // 1=支出 2=收入

private String icon;     // 图标标识，前端拿去显示对应的小图案
}
