package com.tchalanet.server.draw.application.ports.in;

import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawSource;
import java.util.Map;
import java.util.UUID;

public interface ApplyDrawResultUseCase {
  Draw applyResult(ApplyDrawResultCommand cmd);

  record ApplyDrawResultCommand(
      UUID drawId, UUID tenantId, Map<String, Object> payload, DrawSource source, UUID appliedBy) {}
}
