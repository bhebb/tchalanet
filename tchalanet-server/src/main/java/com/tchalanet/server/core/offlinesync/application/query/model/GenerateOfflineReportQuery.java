package com.tchalanet.server.core.offlinesync.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;
import java.time.LocalDate;

public record GenerateOfflineReportQuery(TenantId tenantId, UUID deviceId, LocalDate from, LocalDate to) {}

