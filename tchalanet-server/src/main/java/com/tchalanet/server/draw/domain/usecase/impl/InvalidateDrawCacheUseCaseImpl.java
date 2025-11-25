package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.usecase.InvalidateDrawCacheUseCase;
import com.tchalanet.server.draw.infra.cache.DrawCacheKeyBuilder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawCacheUseCaseImpl implements InvalidateDrawCacheUseCase {

  private final DrawCacheKeyBuilder keyBuilder;
  private final StringRedisTemplate redis;

  @Override
  public void invalidateTenant(UUID tenantId) {
    try {
      String k1 = keyBuilder.today(tenantId);
      String k2 = keyBuilder.last7d(tenantId);
      String k3 = keyBuilder.next(tenantId);
      redis.delete(k1);
      redis.delete(k2);
      redis.delete(k3);
      log.info("Invalidated draw cache for tenant {}: {},{},{}", tenantId, k1, k2, k3);
    } catch (Exception e) {
      log.warn("Failed to invalidate draw cache for tenant {}", tenantId, e);
    }
  }
}
