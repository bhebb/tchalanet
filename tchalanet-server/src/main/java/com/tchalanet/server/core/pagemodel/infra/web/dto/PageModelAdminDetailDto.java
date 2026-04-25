package com.tchalanet.server.core.pagemodel.infra.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelStatus;
import java.time.Instant;

/**
 * DTO de détail admin pour un PageModel (preview, duplicate, reset).
 * Expose tous les champs utiles pour le backoffice sans exposer l'agrégat domaine.
 */
public record PageModelAdminDetailDto(
    String id,
    String tenantId,
    String logicalId,
    String scope,
    String slug,
    PageModelStatus status,
    int schemaVersion,
    JsonNode model,
    String templateId,
    Instant createdAt,
    Instant updatedAt,
    String createdBy,
    String updatedBy,
    Instant publishedAt
) {}

