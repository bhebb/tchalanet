package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.ports.in.ListTodayDrawsUseCase;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListTodayDrawsUseCaseImpl implements ListTodayDrawsUseCase {

  private final DrawRepository drawRepository;

  public List<ChannelSummary> listTodayDraws(UUID tenantId) {
    log.warn("ListTodayDrawsUseCaseImpl is a placeholder and does not implement actual logic.");
    return List.of();
  }
}
