package com.tchalanet.server.core.draw.infra.web.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
@Builder
public class DrawChannelResponse {
  private UUID id;
  private UUID tenantId;
  private String code;
  private String name;
  private String label;
  private String gameCode;
  private String timezone;
  private LocalTime drawTime;
  private Integer cutoffSec;
  private String daysOfWeek;
  private Boolean active;
  private Integer sortOrder;
  private JsonNode flags;
  private String notes;
  private UUID resultSlotId;
  private String defaultSource;
  private Instant createdAt;
  private Instant updatedAt;
}
