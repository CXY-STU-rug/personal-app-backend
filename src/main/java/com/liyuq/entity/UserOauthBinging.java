package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_oauth_binding")
public class UserOauthBinging {
    @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;

    /** 平台标识：github / gitee / google... */
    private String provider;

    /** 该平台里这个用户的唯一 ID（GitHub 的数字 id，不是用户名——用户名可改，id 不变） */
    // 必须叫 providerUid：开了驼峰转下划线后映射到列 provider_uid。
    // 原来写的 provideUid → 映射成 provide_uid，和建表的 provider_uid 差一个 r，插入直接 Unknown column
    private String providerUid;

    // 同理：原来的 createAt → create_at，和 DDL 的 created_at 对不上
    private LocalDateTime createdAt;
}
