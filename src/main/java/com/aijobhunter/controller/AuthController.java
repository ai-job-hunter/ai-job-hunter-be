package com.aijobhunter.controller;

import com.aijobhunter.common.security.JwtTokenProvider;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.domain.user.UserRepository;
import com.aijobhunter.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse.UserDTO>> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(3002, "邮箱已被注册"));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(3002, "用户名已被使用"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .preferences(createDefaultPreferences())
                .automationConfig(createDefaultAutomationConfig())
                .build();

        user = userRepository.save(user);

        AuthResponse.UserDTO userDTO = AuthResponse.UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bossConnected(user.getBossConnected())
                .build();

        return ResponseEntity.ok(ApiResponse.success("注册成功", userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(2001, "邮箱或密码错误"));
        }

        String token = tokenProvider.generateToken(user.getId());

        AuthResponse.UserDTO userDTO = AuthResponse.UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bossConnected(user.getBossConnected())
                .build();

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpiration() / 1000)
                .user(userDTO)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Map<String, Object> createDefaultPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("keywords", new String[]{});
        prefs.put("cities", new String[]{"北京"});
        return prefs;
    }

    private Map<String, Object> createDefaultAutomationConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("mode", "SEMI");
        config.put("dailyGreetLimit", 50);
        config.put("dailyApplyLimit", 20);
        config.put("replyDelaySeconds", 2);
        config.put("autoApply", false);
        return config;
    }
}
