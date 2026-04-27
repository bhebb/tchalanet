package com.tchalanet.server.core.tenantgame.infra.web.model;

import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class UpdatePolicyRequest {
  private JsonNode policy;
}
