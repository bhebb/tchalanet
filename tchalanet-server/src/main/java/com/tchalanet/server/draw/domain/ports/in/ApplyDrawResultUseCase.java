package com.tchalanet.server.draw.domain.ports.in;

import java.util.UUID;

/** Use case pour appliquer un résultat sur un tirage (CLOSED -> RESULTED). */
public interface ApplyDrawResultUseCase {

  void applyResult(UUID tenantId, UUID drawId, String resultPayloadJson);
}
