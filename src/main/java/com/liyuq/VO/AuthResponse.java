package com.liyuq.VO;

import com.liyuq.entity.Users;
import lombok.Data;

@Data
public class AuthResponse {

    private String token;

    private Users user;

    public AuthResponse(String token, Users user) {
        this.token = token;
        this.user = user;
    }
}