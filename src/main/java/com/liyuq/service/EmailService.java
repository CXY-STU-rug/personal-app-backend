package com.liyuq.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
}