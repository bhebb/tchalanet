package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.core.tenantgame.application.command.model.UpdateTenantGamePolicyCommand;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateTenantGamePolicyCommandHandler {

  private final TenantGamePersistencePort persistencePort;

  @Transactional
  public void handle(UpdateTenantGamePolicyCommand command) {
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
        existing.enabled(),
        existing.displayName(),
        existing.minStake(),
        existing.maxStake(),
        command.getPolicy() // updated flags
    );

    persistencePort.save(tenantGame);
  }
}
