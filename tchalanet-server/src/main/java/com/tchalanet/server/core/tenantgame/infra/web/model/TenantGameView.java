package com.tchalanet.server.core.tenantgame.infra.web.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

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
