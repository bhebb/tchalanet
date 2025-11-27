package com.tchalanet.server.draw.infra.adapters;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridge adapter implementing the domain port and delegating to the existing application port.
 * Temporary shim to ease the refactor; should be removed when ports are unified.
 */
@Component
public class ApplyDrawResultBridgeAdapter
    implements com.tchalanet.server.draw.domain.ports.in.ApplyDrawResultUseCase {

  private final com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase delegate;

  public ApplyDrawResultBridgeAdapter(
      com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  public void applyResult(UUID tenantId, UUID drawId, String resultPayloadJson) {
    // Map the JSON string into a simple payload map expected by the application port.
    // This is a temporary bridge; refactor later to avoid lossy conversions.
    Map<String, Object> payload = Map.of("resultJson", resultPayloadJson);
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
