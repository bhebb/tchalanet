package com.tchalanet.server.features.pagemodel.shared.template.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageModelTemplateRequest {
    private UUID tenantId;
    private String logicalId;
    private String label;
    private String description;
    private int schemaVersion;
    private String modelJson;
    private Boolean isDefault;
    private Boolean isSystem;
}

