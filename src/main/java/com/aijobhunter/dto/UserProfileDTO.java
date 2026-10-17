package com.aijobhunter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private String id;
    private String username;
    private String email;
    private String phone;
    private Boolean bossConnected;
    private Map<String, Object> preferences;
    private Map<String, Object> automationConfig;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}
