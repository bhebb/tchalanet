package com.tchalanet.server.core.offlinesync.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

public record GenerateOfflineReportQuery(UUID tenantId, UUID deviceId, LocalDate from, LocalDate to) {}

