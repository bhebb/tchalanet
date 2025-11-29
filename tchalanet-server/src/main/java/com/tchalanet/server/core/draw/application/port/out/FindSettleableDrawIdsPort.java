package com.tchalanet.server.core.draw.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FindSettleableDrawIdsPort {
  List<UUID> findSettleableDrawIds(Instant until);
}
