package com.tchalanet.server.core.tenant.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantGame {
  private TenantGameId id;
  private TenantId tenantId;
  private UUID gameId; // référence sur game.id
  private String gameCode; // optionnel si tu veux encore manipuler le code
  private Boolean enabled;
  private String displayName;
  private BigDecimal minStake;
  private BigDecimal maxStake;
  private Map<String, Object> flags;
  private Instant createdAt;
  private Instant updatedAt;
}
