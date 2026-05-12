package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.core.tenantgame.application.command.model.DisableTenantGameCommand;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DisableTenantGameCommandHandler {

  private final TenantGamePersistencePort persistencePort;

  @Transactional
  public void handle(DisableTenantGameCommand command) {
    var existing = persistencePort.findByTenantIdAndGameCode(command.getTenantId(), command.getGameCode())
        .orElseThrow(() -> new IllegalArgumentException("Tenant game not found"));

    var tenantGame = new TenantGame(
        existing.tenantGameId(),
        existing.tenantId(),
        existing.gameId(),
        existing.code(),
        existing.name(),
        existing.category(),
        existing.minDigits(),
        existing.maxDigits(),
        existing.combination(),
        false, // enabled = false
        existing.displayName(),
        existing.minStake(),
        existing.maxStake(),
        existing.flags()
    );

    persistencePort.save(tenantGame);
  }
}
