package com.aijobhunter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String phone;
    private Map<String, Object> preferences;
    private Map<String, Object> automationConfig;
}
