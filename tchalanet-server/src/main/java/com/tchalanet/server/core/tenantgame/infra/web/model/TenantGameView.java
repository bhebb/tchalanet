package com.tchalanet.server.core.tenantgame.infra.web.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
@Builder
public class TenantGameView {
  private String gameCode;
  private boolean enabled;
  private String displayName;
  private BigDecimal minStake;
  private BigDecimal maxStake;
  private JsonNode flags;
}
