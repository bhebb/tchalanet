package com.tchalanet.server.features.pagemodel.application.admin;

import java.time.Instant;
import java.util.UUID;

public record PageModelSummaryDto(
    UUID id, UUID tenantId, String code, String lang, Instant updatedAt) {}
