package com.tchalanet.server.draw.application.port.out;

import com.tchalanet.server.draw.application.query.model.DrawResultOverrideMetadata;
import com.tchalanet.server.draw.domain.model.DrawResult;
import java.util.UUID;

public interface DrawResultWriterPort {
  DrawResult save(DrawResult result);

  DrawResult save(UUID tenantId, UUID drawId, DrawResult result);

  // pour override / invalidate si tu veux être explicite
  DrawResult overrideResult(DrawResult result, DrawResultOverrideMetadata metadata);

  DrawResult invalidateResult(UUID tenantId, UUID drawId, String reason);
}
