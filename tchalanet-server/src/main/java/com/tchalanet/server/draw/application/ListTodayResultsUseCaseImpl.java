package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.ports.in.ListTodayResultsUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListTodayResultsUseCaseImpl implements ListTodayResultsUseCase {

  public List<ChannelSummary> listTodayResults(UUID tenantId) {
    log.warn("ListTodayResultsUseCaseImpl is a placeholder and does not implement actual logic.");
    return List.of();
  }
}
