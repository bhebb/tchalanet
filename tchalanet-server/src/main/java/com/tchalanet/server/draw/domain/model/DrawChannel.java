package com.tchalanet.server.draw.domain.model;

import com.tchalanet.server.common.domain.DrawChannelId;
import com.tchalanet.server.common.domain.TenantId;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DrawChannel {
  private DrawChannelId id;
  private TenantId tenantId;
  private String code;
  private String name;
  private UUID gameId; // référence forte vers game.id
  private String gameCode; // optionnel : peut être rempli par un use case en joignant game
  private String timezone;
  private LocalTime drawTime;
  private Integer cutoffSec;
  private String daysOfWeek;
  private Boolean active;
  private Integer sortOrder;
  private Instant createdAt;
  private Instant updatedAt;
}
