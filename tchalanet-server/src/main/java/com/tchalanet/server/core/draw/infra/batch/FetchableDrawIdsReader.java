package com.tchalanet.server.core.draw.infra.batch;

import com.tchalanet.server.core.draw.application.port.out.FindFetchableDrawIdsPort;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.stereotype.Component;

/** Reader qui délègue à un port out pour trouver les tirages à traiter. */
@Component
public class FetchableDrawIdsReader extends IteratorItemReader<UUID> {

  public FetchableDrawIdsReader(FindFetchableDrawIdsPort port, Clock clock) {
    super(fetch(port, clock));
  }

  private static List<UUID> fetch(FindFetchableDrawIdsPort port, Clock clock) {
    var from = Instant.now(clock).minus(Period.ofDays(6));
    // todo check
    return port.findFetchableDrawIds(from);
  }
}
