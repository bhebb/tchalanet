package com.tchalanet.server.common.cache;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CacheKeyBuilder {
  private final String env;

  public CacheKeyBuilder(@Value("${tch.env:${spring.profiles.active:dev}}") String env) {
    this.env = env == null ? "dev" : env;
  }

  public String tenantConfigKey(UUID tenantId) {
    return "tch:%s:%s:tenant:config".formatted(env, tenantId);
  }

  public String tenantThemeKey(UUID tenantId) {
    return "tch:%s:%s:theme:active".formatted(env, tenantId);
  }

  public String tenantDrawsSummaryKey(UUID tenantId) {
    return "tch:%s:%s:draws:summary".formatted(env, tenantId);
  }

  public String tenantDrawsChannelKey(UUID tenantId, String channelCode, String kind) {
    return "tch:%s:%s:draws:%s:%s".formatted(env, tenantId, channelCode, kind);
  }

  public String globalSearchKey(String queryHash) {
    return "tch:%s:-:search:query:%s".formatted(env, queryHash);
  }
}
