package com.tchalanet.server.platform.tenantgame.internal.web;

import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class UpdatePolicyRequest {
  private JsonNode policy;
}
