package com.tchalanet.server.draw.infra.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DrawSummaryResponse(UUID drawId, String channelCode, LocalDateTime scheduledAt) {}
