package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import java.time.Instant;
import java.util.Optional;

public interface DrawLookupPort {
  Optional<DrawSnapshot> findById(DrawId drawId);

  record DrawSnapshot(
      DrawId drawId,
      DrawChannelId drawChannelId,
      Instant cutoffAt,
      Instant resultedAt
  ) {}
}

