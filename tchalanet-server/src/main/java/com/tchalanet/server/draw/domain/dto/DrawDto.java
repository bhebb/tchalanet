package com.tchalanet.server.draw.domain.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DrawDto(
    UUID id,
    UUID channelId,
    String channelCode,
    OffsetDateTime scheduledAt,
    OffsetDateTime resultAt,
    List<Integer> numbers,
    Map<String, Object> meta) {}
