package com.aijobhunter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResumeRequest {
    private String name;
    private Boolean isDefault;
    private String type;
}
