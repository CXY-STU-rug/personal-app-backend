package com.liyuq.DTO;


import lombok.Data;

@Data
public class RegisterDto {
    private String username;
    private String password;
    private String nickname;   // 可选，不传则后端默认取username（接口文档3.1）
}
