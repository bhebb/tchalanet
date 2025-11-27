package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.CloseDueDrawsUseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.ticket.domain.ports.out.ClockPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseDueDrawsUseCaseImpl implements CloseDueDrawsUseCase {

  private final DrawRepository drawRepository;
  private final ClockPort clock;

  @Transactional
  public void closeDueDraws(UUID tenantId) {
    log.info("Closing due draws for tenant {}", tenantId);
    Instant now = clock.now();

    // Find all SCHEDULED draws that are past their cutoff time
    List<Draw> dueDraws = drawRepository.findScheduledDrawsPastCutoff(tenantId, now);

    int closedCount = 0;
    for (Draw draw : dueDraws) {
      try {
        // Assuming a system user ID for this automated action
        Draw closedDraw =
            draw.withStatus(DrawStatus.CLOSED)
                .withUpdatedBy(
                    UUID.fromString("00000000-0000-0000-0000-000000000001")); // System User ID
        drawRepository.save(closedDraw);
        closedCount++;
        log.info(
            "Closed draw {} for channel {} (tenant {})",
            draw.id(),
            draw.drawChannelId(),
            draw.tenantId());
      } catch (Exception e) {
        log.error("Failed to close draw {}: {}", draw.id(), e.getMessage(), e);
      }
    }
    log.info("Finished closing due draws. {} draws closed.", closedCount);
  }
}
