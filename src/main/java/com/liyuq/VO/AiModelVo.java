package com.liyuq.VO;

import lombok.Data;

/**
 * 可用AI模型（接口文档3.31返回数组的元素）。
 * 前端"模型切换器"下拉里的一项——有哪些模型可选由后端说了算，前端不写死。
 */
@Data
public class AiModelVo {

    private String id;            // 模型标识，就是调DeepSeek时传的model值，也是3.28请求里model字段的合法取值

    private String name;          // 展示名（中文），切换器按钮/下拉里显示

    private String description;   // 一句话特点，帮用户选

    // 是否默认模型。用包装类型Boolean（不是boolean）：Lombok给它生成的getter是getIsDefault()，
    // Jackson据此序列化出的JSON键才是"isDefault"；若用基本类型boolean，getter叫isDefault()，
    // Jackson会把键写成"default"，和合同对不上——这是boolean字段序列化的经典坑
    private Boolean isDefault;
}
