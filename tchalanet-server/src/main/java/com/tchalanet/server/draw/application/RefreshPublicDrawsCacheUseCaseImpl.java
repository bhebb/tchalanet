package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.RefreshPublicDrawsCacheUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class RefreshPublicDrawsCacheAppService implements RefreshPublicDrawsCacheUseCase {

  @Override
  public void refreshCache(UUID tenantId) {
    log.warn(
        "RefreshPublicDrawsCacheAppService is a placeholder and does not implement actual cache refresh logic.");
  }
}
