package com.aijobhunter.service;

import com.aijobhunter.domain.blacklist.Blacklist;
import com.aijobhunter.domain.blacklist.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> list(String userId) {
        List<Map<String, Object>> companies = new ArrayList<>();
        List<Map<String, Object>> keywords = new ArrayList<>();
        List<Map<String, Object>> jobTitles = new ArrayList<>();
        List<Map<String, Object>> hrs = new ArrayList<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Blacklist item : blacklistRepository.findByUserIdOrderByAddedAtDesc(userId)) {
            Map<String, Object> dto = toDto(item);
            items.add(dto);
            switch (item.getType()) {
                case COMPANY -> companies.add(dto);
                case JOB_TITLE -> jobTitles.add(dto);
                case HR -> hrs.add(dto);
                default -> keywords.add(dto);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("companies", companies);
        data.put("keywords", keywords);
        data.put("jobTitles", jobTitles);
        data.put("hrs", hrs);
        data.put("items", items);
        return data;
    }

    @Transactional
    public Map<String, Object> add(String userId, String type, String value, String reason, LocalDateTime expiresAt) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("黑名单内容不能为空");
        }
        Blacklist.BlacklistType t = parseType(type);
        String trimmed = value.trim();
        if (blacklistRepository.existsByUserIdAndTypeAndValueIgnoreCase(userId, t, trimmed)) {
            throw new IllegalStateException("该项已在黑名单中");
        }
        Blacklist item = Blacklist.builder()
                .userId(userId)
                .type(t)
                .value(trimmed)
                .reason(reason == null || reason.isBlank() ? null : reason.trim())
                .expiresAt(expiresAt)
                .build();
        item = blacklistRepository.save(item);
        return toDto(item);
    }

    @Transactional
    public void delete(String userId, String id) {
        Blacklist item = blacklistRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("黑名单项不存在"));
        blacklistRepository.delete(item);
    }

    private Map<String, Object> toDto(Blacklist item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("type", item.getType() == null ? null : item.getType().name());
        data.put("value", item.getValue());
        data.put("reason", item.getReason());
        data.put("addedAt", item.getAddedAt());
        data.put("expiresAt", item.getExpiresAt());
        return data;
    }

    private static Blacklist.BlacklistType parseType(String type) {
        if (type == null || type.isBlank()) {
            return Blacklist.BlacklistType.KEYWORD;
        }
        try {
            return Blacklist.BlacklistType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type 仅支持 COMPANY / JOB_TITLE / KEYWORD / HR");
        }
    }
}
