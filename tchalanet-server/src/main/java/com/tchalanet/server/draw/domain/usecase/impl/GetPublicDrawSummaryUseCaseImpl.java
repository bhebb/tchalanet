package com.tchalanet.server.draw.domain.usecase.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.dto.DrawDto;
import com.tchalanet.server.draw.domain.dto.NextDrawDto;
import com.tchalanet.server.draw.domain.dto.PublicDrawSummary;
import com.tchalanet.server.draw.domain.ports.DrawQueryPort;
import com.tchalanet.server.draw.domain.usecase.GetPublicDrawSummaryUseCase;
import com.tchalanet.server.draw.infra.cache.DrawCacheKeyBuilder;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetPublicDrawSummaryUseCaseImpl implements GetPublicDrawSummaryUseCase {

  private final DrawQueryPort drawQuery;
  private final DrawCacheKeyBuilder keyBuilder;
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;

  @Value("${tch.draw.cache.ttl.last7m:${draw.cache.ttl.last7m:5}}")
  private int last7m;

  @Value("${tch.draw.cache.ttl.todaym:${draw.cache.ttl.todaym:5}}")
  private int todaym;

  @Value("${tch.draw.cache.ttl.nexts:${draw.cache.ttl.nexts:60}}")
  private int nexts;

  @Override
  public PublicDrawSummary getSummaryForTenant(java.util.UUID tenantId) {
    OffsetDateTime serverTime = OffsetDateTime.now();

    String keyToday = keyBuilder.today(tenantId);
    String keyLast7 = keyBuilder.last7d(tenantId);
    String keyNext = keyBuilder.next(tenantId);

    List<DrawDto> today = readList(keyToday, new TypeReference<List<DrawDto>>() {});
    Map<UUID, List<DrawDto>> last7 =
        readMap(keyLast7, new TypeReference<Map<UUID, List<DrawDto>>>() {});
    List<NextDrawDto> next = readList(keyNext, new TypeReference<List<NextDrawDto>>() {});

    if (today == null) {
      today = drawQuery.findResultedDrawsForLastDays(tenantId, 1);
      write(keyToday, today, Duration.ofMinutes(todaym));
    }

    if (last7 == null) {
      // prefer last N per channel (7) using optimized query
      last7 = drawQuery.findLastNPerChannel(tenantId, 7);
      write(keyLast7, last7, Duration.ofMinutes(last7m));
    }

    if (next == null) {
      Map<UUID, NextDrawDto> nextMap = drawQuery.findNextDrawPerChannel(tenantId);
      next = new ArrayList<>(nextMap.values());
      write(keyNext, next, Duration.ofSeconds(nexts));
    }

    return new PublicDrawSummary(tenantId, serverTime, today, last7, next);
  }

  private <T> T readList(String key, TypeReference<T> type) {
    try {
      String json = redis.opsForValue().get(key);
      if (json == null || json.isBlank()) return null;
      return mapper.readValue(json, type);
    } catch (Exception e) {
      log.warn("Failed to read key {} from redis", key, e);
      return null;
    }
  }

  private void write(String key, Object payload, Duration ttl) {
    try {
      String json = mapper.writeValueAsString(payload);
      redis.opsForValue().set(key, json, ttl);
    } catch (Exception e) {
      log.warn("Failed to write key {} to redis", key, e);
    }
  }

  private <T> T readMap(String key, TypeReference<T> type) {
    return readList(key, type);
  }
}
