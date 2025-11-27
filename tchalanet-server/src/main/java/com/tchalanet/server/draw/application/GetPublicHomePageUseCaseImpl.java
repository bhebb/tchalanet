package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.DrawSummary;
import com.tchalanet.server.draw.application.ports.in.GetPublicHomePageUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPublicHomePageUseCaseImpl implements GetPublicHomePageUseCase {

  public Optional<DrawSummary> getPublicHomePage(UUID tenantId) {
    log.warn("GetPublicHomePageUseCaseImpl is a placeholder and does not implement actual logic.");
    return Optional.empty();
  }
}
