package com.tchalanet.server.draw.application.adapters;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ApplyDrawResultDomainAdapter
    implements com.tchalanet.server.draw.domain.ports.in.ApplyDrawResultUseCase {

  private final com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase delegate;

  public ApplyDrawResultDomainAdapter(
      com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  public void applyResult(UUID tenantId, UUID drawId, String resultPayloadJson) {
    java.util.Map<String, Object> payload = java.util.Map.of("resultJson", resultPayloadJson);
    var cmd =
        new com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase
            .ApplyDrawResultCommand(
            drawId,
            tenantId,
            payload,
            com.tchalanet.server.draw.domain.model.DrawSource.SYSTEM,
            null);
    delegate.applyResult(cmd);
  }
}
