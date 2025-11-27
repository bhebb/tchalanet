package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.DrawSummary;
import com.tchalanet.server.draw.application.ports.in.GetPublicDrawsSummaryUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPublicDrawsSummaryUseCaseImpl implements GetPublicDrawsSummaryUseCase {

  public Optional<DrawSummary> getPublicDrawsSummary(UUID tenantId) {
    log.warn(
        "GetPublicDrawsSummaryUseCaseImpl is a placeholder and does not implement actual logic.");
    return Optional.empty();
  }
}
