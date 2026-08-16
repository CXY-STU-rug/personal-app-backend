package com.liyuq.service.impl;

import com.liyuq.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

@Autowired
private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {

        try{
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("重置密码");
        message.setText("请点击以下链接重置密码，链接 30 分钟内有效：\n" + resetLink);
        mailSender.send(message);
    }
    catch (Exception e) {
        // 异步线程里的异常不会传回调用方，不自己打日志就彻底丢了，
        // 到时候用户说"没收到邮件"你完全查不到原因
     log.error("发送重置邮件失败，目标邮箱：{}“，", to, e);
    }
}}