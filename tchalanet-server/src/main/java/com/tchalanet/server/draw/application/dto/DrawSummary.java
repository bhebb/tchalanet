package com.tchalanet.server.draw.application.dto;

import com.tchalanet.server.draw.domain.model.ChannelSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for summarizing draw information, typically for display purposes. Moved from domain.model to
 * application.dto.
 */
public record DrawSummary(UUID tenantId, List<ChannelSummary> channels, Map<String, Object> meta) {}
