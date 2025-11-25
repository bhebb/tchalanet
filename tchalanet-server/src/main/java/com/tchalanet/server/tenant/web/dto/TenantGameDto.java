package com.tchalanet.server.tenant.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantGameDto {
  private UUID id;
  private UUID tenantId;
  private String gameCode;
  private Boolean enabled;
  private String displayName;
  private java.math.BigDecimal minStake;
  private java.math.BigDecimal maxStake;
  private Map<String, Object> flags;
  private Instant createdAt;
  private Instant updatedAt;
}
