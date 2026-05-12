package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenanttheme.application.command.model.DeactivateTenantThemeCommand;
import com.tchalanet.server.core.tenanttheme.application.event.TenantThemeUpdatedEvent;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemePersistencePort;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemeReaderPort;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Handler for DeactivateTenantThemeCommand.
 * Maps to spec requirement T5.
 */
@UseCase
@RequiredArgsConstructor
public class DeactivateTenantThemeCommandHandler
    implements VoidCommandHandler<DeactivateTenantThemeCommand> {

  private final TenantThemePersistencePort persistencePort;
  private final TenantThemeReaderPort readerPort;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(DeactivateTenantThemeCommand cmd) {
    // Check if theme exists
    var existing = readerPort.findByTenantId(cmd.tenantId());

    if (existing.isEmpty()) {
      // Already deactivated or never had theme
      return;
    }

    // Deactivate
    persistencePort.deactivate(cmd.tenantId());

    // Publish event AFTER COMMIT
    var theme = existing.get();
    AfterCommit.run(() -> {
      var event =
          new TenantThemeUpdatedEvent(
              theme.tenantId(),
              null, // deactivated
              theme.version() + 1,
              Instant.now(clock),
              "system");
      eventPublisher.publishEvent(event);
    });
  }
}
