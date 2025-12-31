package com.tchalanet.server.core.game.infra.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantGameUpdateRequest {
  private Boolean enabled;
  @Size(max = 128)
  private String displayName;
  private BigDecimal minStake;
  private BigDecimal maxStake;
  private JsonNode flags;
}
