package com.tchalanet.server.core.draw.infra.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDrawRequest(UUID tenantId, String channelCode, LocalDate scheduledDate) {}
