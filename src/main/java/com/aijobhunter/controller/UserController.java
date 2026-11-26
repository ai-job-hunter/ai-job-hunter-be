package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.domain.user.UserRepository;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.dto.ChangePasswordRequest;
import com.aijobhunter.dto.UpdateUserRequest;
import com.aijobhunter.dto.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> me() {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(toDto(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateMe(@RequestBody UpdateUserRequest request) {
        User user = SecurityUtils.requireCurrentUser();

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
        }
        if (request.getPreferences() != null) {
            user.setPreferences(request.getPreferences());
        }
        if (request.getAutomationConfig() != null) {
            user.setAutomationConfig(request.getAutomationConfig());
        }
        user.setLastActiveAt(LocalDateTime.now());
        user = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("更新成功", toDto(user)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @RequestBody ChangePasswordRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        if (request == null
                || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()
                || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("请填写当前密码和新密码");
        }
        String current = request.getCurrentPassword();
        String next = request.getNewPassword().trim();
        if (next.length() < 8) {
            throw new IllegalArgumentException("新密码至少 8 位");
        }
        if (next.length() > 72) {
            throw new IllegalArgumentException("新密码过长");
        }
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (passwordEncoder.matches(next, user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(next));
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("密码已更新", Map.of("ok", true)));
    }

    private static UserProfileDTO toDto(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bossConnected(user.getBossConnected())
                .preferences(user.getPreferences())
                .automationConfig(user.getAutomationConfig())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
