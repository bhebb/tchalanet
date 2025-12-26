package com.tchalanet.server.core.draw.infra.web.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class DrawChannelResponse {
    private UUID id;
    private UUID tenantId;
    private String code;
    private String name;
    private String gameCode;
    private String timezone;
    private LocalTime drawTime;
    private Integer cutoffSec;
    private String daysOfWeek;
    private Boolean active;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
