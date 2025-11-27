package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.SettleDrawsUseCase;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettleDrawsUseCaseImpl implements SettleDrawsUseCase {

  private final DrawRepository drawRepository;

  public void settleDraws(UUID tenantId) {
    log.warn(
        "SettleDrawsUseCaseImpl is a placeholder and does not implement actual settlement logic.");
    // In a real implementation, this would:
    // 1. Find draws that are in a 'RESULTED' state but not yet settled.
    // 2. Fetch all tickets associated with these draws.
    // 3. Compare ticket selections with draw results to determine WON/LOST.
    // 4. Update ticket status and calculate actual payouts.
    // 5. Publish TicketSettledEvent.
  }
}
