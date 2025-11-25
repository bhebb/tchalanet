package com.tchalanet.server.draw.domain.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.usecase.GetNextDrawsUseCase;
import com.tchalanet.server.draw.domain.usecase.ListLast7DaysResultsUseCase;
import com.tchalanet.server.draw.domain.usecase.ListTodayResultsUseCase;
import com.tchalanet.server.draw.domain.usecase.RefreshPublicDrawCacheUseCase;
import com.tchalanet.server.draw.infra.cache.DrawCacheKeyBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicDrawCacheUseCaseImpl implements RefreshPublicDrawCacheUseCase {

  private final ListTodayResultsUseCase todayUse;
  private final ListLast7DaysResultsUseCase last7Use;
  private final GetNextDrawsUseCase nextUse;
  private final DrawCacheKeyBuilder keyBuilder;
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;

  @Value("${draw.cache.ttl.last7m:5}")
  private int last7m;

  @Value("${draw.cache.ttl.todaym:5}")
  private int todaym;

  @Value("${draw.cache.ttl.nexts:60}")
  private int nexts;

  @Override
  public void execute() {
    // fetch all tenants (simple approach: find tenants from DB). For now, we assume a default
    // tenant list is provided elsewhere.
    log.info("Refreshing public draw cache - start");
    // TODO: iterate tenants -> compute and store
  }

  // helper to write list to redis with TTL
  private void write(UUID tenantId, String key, List<Map<String, Object>> payload, Duration ttl) {
    try {
      var json = mapper.writeValueAsString(payload);
      redis.opsForValue().set(key, json, ttl);
    } catch (Exception e) {
      log.warn("Failed to write draw cache {}", key, e);
    }
  }
}
