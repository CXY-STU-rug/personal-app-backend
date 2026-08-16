package com.liyuq.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {

    // 邮件链接里带的明文令牌。原来走 @RequestParam 在 URL 上，
    // 会被 Nginx/Tomcat 的 access log 原文记录，改成走请求体
    @NotBlank(message = "令牌不能为空")
    private String token;

    // 字段名跟文档 3.48 保持一致：newPassword，不是 password
    // 6-50 位是文档定的限制。不校验的话 ?password= 空串也能通过，
    // 账号密码会被设成空串的 bcrypt 值，等于被锁死
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50位之间")
    private String newPassword;
}
