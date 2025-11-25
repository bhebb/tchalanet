package com.tchalanet.server.draw.domain.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DrawSummary(UUID tenantId, List<ChannelSummary> channels, Map<String, Object> meta) {}
