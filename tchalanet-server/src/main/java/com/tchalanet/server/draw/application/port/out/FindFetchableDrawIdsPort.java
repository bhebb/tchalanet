package com.tchalanet.server.draw.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FindFetchableDrawIdsPort {
  List<UUID> findFetchableDrawIds(Instant since);
}
