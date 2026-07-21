package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI对话记录实体（对应 ai_messages 表，接口文档1节的建表SQL）
 * 一行 = 一条消息：用户说的一句 或 AI回的一句，各占一行
 */
@Data                          // Lombok自动生成getter/setter/toString
@TableName("ai_messages")      // 显式声明表名（类名AiMessages驼峰转下划线本来也能对上，写明白不猜）
public class AiMessages {

    @TableId(type = IdType.AUTO)   // 主键交给MySQL自增；自增id天然就是时间顺序，拉历史按它排序
    private Long id;

    private Long userId;           // 数据隔离：每条SQL都必须带它过滤（映射user_id列，驼峰转换在yml开了）

    private String role;           // user / assistant——故意和DeepSeek API同名同值，查出来直接当上下文用

    private String content;        // 消息正文（表里是TEXT，能装长回复）

    private LocalDateTime createdAt;   // 创建时间，insert时后端自己set，不收前端的
}
