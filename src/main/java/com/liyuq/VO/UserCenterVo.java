package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCenterVo {


private Long id;

private String username;

private String nickname;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdAt;   // 注册时间：字段名对齐接口文档3.19（原来拼成createAt少个d）

}
