package com.tchalanet.server.draw.application.query;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.ports.in.query.GetNextDrawQuery;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetNextDrawService implements GetNextDrawQuery {

  // private final DrawRepository drawRepository; // Placeholder
  // private final DrawChannelRepository drawChannelRepository; // Placeholder

  @Override
  public Optional<ChannelSummary> getNextDraw(UUID tenantId, String gameCode) {
    log.warn("GetNextDrawService is a placeholder and does not implement actual logic.");
    // In a real implementation, this would query the next scheduled draw
    // for the given tenant and game code.
    return Optional.empty();
  }
}
