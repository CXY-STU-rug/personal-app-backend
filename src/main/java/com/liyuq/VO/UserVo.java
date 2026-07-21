package com.liyuq.VO;

import lombok.Data;

// 登录接口的返回体，字段对齐接口文档3.2：token + userId + nickname
@Data
public class UserVo {
    private String token;
    private Long userId;
    private String nickname;
}
