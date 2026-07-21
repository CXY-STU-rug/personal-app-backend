package com.liyuq.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("users")   // 必须和数据库表名一字不差：库里的表叫 users，之前写成 user 导致所有SQL报"表不存在"
public class Users {
    @TableId(type = IdType.AUTO)
    private Long id;//用户id，BIGINT对应Long

    private String username;  //登录用户名

    private String password;// 登录的密码，bcrypt密文，永不存明文

    private String nickname;//用户的昵称

    private String avatar;//用户头像url

    private LocalDateTime createdAt;//注册时间，驼峰createdAt ↔ 列名created_at自动互转（之前少个d对不上）

    private LocalDateTime updatedAt;//信息更新时间
}
