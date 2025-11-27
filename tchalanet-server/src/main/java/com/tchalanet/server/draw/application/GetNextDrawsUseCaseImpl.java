package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.ports.in.GetNextDrawsUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetNextDrawsUseCaseImpl implements GetNextDrawsUseCase {

  public List<ChannelSummary> getNextDraws(UUID tenantId, int limit) {
    log.warn("GetNextDrawsUseCaseImpl is a placeholder and does not implement actual logic.");
    return List.of();
  }
}
