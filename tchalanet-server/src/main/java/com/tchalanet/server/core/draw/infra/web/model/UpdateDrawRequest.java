package com.tchalanet.server.core.draw.infra.web.model;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateDrawRequest(UUID tenantId, UUID drawId, LocalDate scheduledDate) {}
