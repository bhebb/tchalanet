package com.tchalanet.server.core.offlinesync.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

public record GetOfflineQueueDetailsQuery(UUID tenantId, UUID deviceId, int page, int size) {}

