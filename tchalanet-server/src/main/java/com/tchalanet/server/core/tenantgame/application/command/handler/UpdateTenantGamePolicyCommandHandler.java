package com.tchalanet.server.core.tenantgame.application.command.handler;

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

    var tenantGame = TenantGame.builder()
        .tenantId(existing.getTenantId())
        .gameCode(existing.getGameCode())
        .enabled(existing.isEnabled())
        .displayName(existing.getDisplayName())
        .minStake(existing.getMinStake())
        .maxStake(existing.getMaxStake())
        .flags(command.getPolicy())
        .version(existing.getVersion())
        .build();

    persistencePort.save(tenantGame);
  }
}
