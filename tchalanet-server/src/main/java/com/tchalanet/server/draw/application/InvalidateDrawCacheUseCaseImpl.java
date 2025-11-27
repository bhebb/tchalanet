package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.InvalidateDrawCacheUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawCacheUseCaseImpl implements InvalidateDrawCacheUseCase {

  public void invalidateCache(UUID tenantId) {
    log.warn(
        "InvalidateDrawCacheUseCaseImpl is a placeholder and does not implement actual cache invalidation logic.");
  }

  @Override
  public void invalidateTenant(UUID tenantId) {}
}
