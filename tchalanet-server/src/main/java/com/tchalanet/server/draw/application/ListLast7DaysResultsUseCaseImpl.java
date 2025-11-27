package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.ports.in.ListLast7DaysResultsUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListLast7DaysResultsUseCaseImpl implements ListLast7DaysResultsUseCase {

  public List<ChannelSummary> listLast7DaysResults(UUID tenantId) {
    log.warn(
        "ListLast7DaysResultsUseCaseImpl is a placeholder and does not implement actual logic.");
    return List.of();
  }
}
