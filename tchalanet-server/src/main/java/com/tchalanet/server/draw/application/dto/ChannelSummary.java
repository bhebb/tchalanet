package com.tchalanet.server.draw.application.dto;

import com.tchalanet.server.draw.domain.model.DrawStatus; // Use the enum
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for summarizing a single draw channel's information. Moved from domain.model to
 * application.dto.
 */
public record ChannelSummary(
    String channelCode,
    String channelName,
    DrawStatus status, // Using DrawStatus enum
    OffsetDateTime drawTime,
    OffsetDateTime cutoffTime,
    boolean isNext,
    List<Integer> lastResult) {}
