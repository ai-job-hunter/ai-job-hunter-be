package com.aijobhunter.common.security;

import com.aijobhunter.domain.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 取当前登录用户（JwtAuthenticationFilter 写入的 principal）。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("未登录或登录已失效");
        }
        return user;
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
