package com.tchalanet.server.features.pagemodel.shared.template.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageModelTemplateResponse {
    private UUID id;
    private UUID tenantId;
    private String logicalId;
    private String label;
    private String description;
    private int schemaVersion;
    private String modelJson;
    private boolean isDefault;
    private boolean isSystem;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private long version;
}

