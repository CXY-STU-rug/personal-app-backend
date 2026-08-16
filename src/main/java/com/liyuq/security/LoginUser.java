package com.liyuq.security;

import com.liyuq.entity.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class LoginUser implements UserDetails {

    private final Users user ;

    public LoginUser(Users user) {
        this.user = user;
    }

    /** 认证成功后 Controller 用它拿 id/role 去签 JWT */
    public Users getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return    user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
