package com.liyuq.DTO;

import lombok.Data;

/**
 * AI对话入参（接口文档3.28的Body）
 * 整个V3只有这一个DTO：天气3.27的city挂在URL上用@RequestParam接，不走请求体
 */
@Data
public class AiChatDto {

    private String content;   // 用户说的话，契约限制1~2000字（校验放service做）
                              // 老规矩：DTO是字段白名单，绝不放userId——身份只从token取

    private String model;     // 用哪个模型（可选，取值来自3.31的id）。安全红线：service必须校验它在
                              // 白名单内，不合法就回落默认，绝不能把前端字符串直接透传给DeepSeek的model参数
}
