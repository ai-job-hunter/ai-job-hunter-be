package com.aijobhunter.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    /** 邮箱或用户名（推荐） */
    private String account;
    /** 兼容旧客户端：等同 account */
    private String email;
    /** 兼容旧客户端：等同 account */
    private String username;
    private String password;

    /** 解析登录标识：account > email > username */
    public String resolveAccount() {
        if (account != null && !account.isBlank()) {
            return account.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return null;
    }
}
