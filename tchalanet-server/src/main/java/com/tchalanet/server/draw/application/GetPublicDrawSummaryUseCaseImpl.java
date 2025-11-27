package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.DrawSummary;
import com.tchalanet.server.draw.application.ports.in.GetPublicDrawSummaryUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPublicDrawSummaryUseCaseImpl implements GetPublicDrawSummaryUseCase {

  public Optional<DrawSummary> getPublicDrawSummary(UUID tenantId) {
    log.warn(
        "GetPublicDrawSummaryUseCaseImpl is a placeholder and does not implement actual logic.");
    return Optional.empty();
  }
}
