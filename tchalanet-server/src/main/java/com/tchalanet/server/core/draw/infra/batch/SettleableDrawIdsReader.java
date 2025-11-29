package com.tchalanet.server.core.draw.infra.batch;

import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.stereotype.Component;

/**
 * Reader qui renvoie les IDs de tirages RESULTED & PENDING dont le cutoff / settlement_due_at est
 * passé.
 */
@Component
public class SettleableDrawIdsReader extends IteratorItemReader<UUID> {

  public SettleableDrawIdsReader(FindSettleableDrawIdsPort port, Clock clock) {
    super(fetch(port, clock));
  }

  private static List<UUID> fetch(FindSettleableDrawIdsPort port, Clock clock) {
    var from = Instant.now(clock).minus(Period.ofDays(6));
    return port.findSettleableDrawIds(from);
  }
}
