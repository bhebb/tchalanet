package com.tchalanet.server.draw.infra.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDrawRequest(UUID tenantId, String channelCode, LocalDate scheduledDate) {}
