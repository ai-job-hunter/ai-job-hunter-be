package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.domain.user.UserRepository;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.dto.UpdateUserRequest;
import com.aijobhunter.dto.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
