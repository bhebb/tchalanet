package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.types.id.TenantId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DrawCacheKeyBuilder {

  @Value("${app.env:dev}")
  private String env;

  public String today(TenantId tenantId) {
    return String.format("tch:%s:%s:draws:today", env, tenantId);
  }

  public String next(TenantId tenantId) {
    return String.format("tch:%s:%s:draws:next", env, tenantId);
  }
}
