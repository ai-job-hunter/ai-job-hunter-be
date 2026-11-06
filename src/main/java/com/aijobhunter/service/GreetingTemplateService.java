package com.aijobhunter.service;

import com.aijobhunter.domain.template.GreetingTemplate;
import com.aijobhunter.domain.template.GreetingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GreetingTemplateService {

    private final GreetingTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String userId) {
        return templateRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(String userId, String name, String content, String description, Boolean isDefault) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
        boolean makeDefault = Boolean.TRUE.equals(isDefault);
        if (makeDefault) {
            templateRepository.clearDefaultForUser(userId);
        }
        GreetingTemplate t = GreetingTemplate.builder()
                .userId(userId)
                .name(name.trim())
                .content(content.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .isDefault(makeDefault)
                .isActive(true)
                .build();
        return toDto(templateRepository.save(t));
    }

    @Transactional
    public Map<String, Object> update(String userId, String id, String name, String content,
                                      String description, Boolean isDefault, Boolean isActive) {
        GreetingTemplate t = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        if (name != null && !name.isBlank()) {
            t.setName(name.trim());
        }
        if (content != null && !content.isBlank()) {
            t.setContent(content.trim());
        }
        if (description != null) {
            t.setDescription(description.isBlank() ? null : description.trim());
        }
        if (Boolean.TRUE.equals(isDefault)) {
            templateRepository.clearDefaultForUser(userId);
            t.setIsDefault(true);
        } else if (Boolean.FALSE.equals(isDefault)) {
            t.setIsDefault(false);
        }
        if (isActive != null) {
            t.setIsActive(isActive);
        }
        return toDto(templateRepository.save(t));
    }

    @Transactional
    public void delete(String userId, String id) {
        GreetingTemplate t = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        templateRepository.delete(t);
    }

    private Map<String, Object> toDto(GreetingTemplate t) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", t.getId());
        data.put("name", t.getName());
        data.put("description", t.getDescription());
        data.put("type", t.getType());
        data.put("content", t.getContent());
        data.put("useCount", t.getUseCount());
        data.put("isDefault", t.getIsDefault());
        data.put("isActive", t.getIsActive());
        data.put("createdAt", t.getCreatedAt());
        data.put("updatedAt", t.getUpdatedAt());
        return data;
    }
}
