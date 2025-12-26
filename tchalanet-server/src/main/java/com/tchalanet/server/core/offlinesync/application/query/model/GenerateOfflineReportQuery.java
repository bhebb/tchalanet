package com.tchalanet.server.core.offlinesync.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateOfflineReportQuery(
    TenantId tenantId, UUID deviceId, LocalDate from, LocalDate to) {}
