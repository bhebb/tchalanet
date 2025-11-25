package com.tchalanet.server.draw.domain.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicDrawSummary(
    UUID tenantId,
    OffsetDateTime serverTime,
    List<DrawDto> todayResults,
    Map<UUID, List<DrawDto>> last7ByChannel,
    List<NextDrawDto> nextByChannel) {}
