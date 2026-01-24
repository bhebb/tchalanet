package com.tchalanet.server.core.tenantgame.infra.web.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UpdatePolicyRequest {
  private JsonNode policy;
}
