package com.tchalanet.server.core.tenanttheme.application.command.handler;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenanttheme.application.command.model.ApplyTenantThemeCommand;
import com.tchalanet.server.core.tenanttheme.application.event.TenantThemeUpdatedEvent;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemePersistencePort;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemeReaderPort;
import com.tchalanet.server.core.tenanttheme.domain.model.TenantTheme;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Handler for ApplyTenantThemeCommand.
 * Maps to spec requirement T1, T3, T4.
 */
@UseCase
@RequiredArgsConstructor
public class ApplyTenantThemeCommandHandler
    implements VoidCommandHandler<ApplyTenantThemeCommand> {

  private final ThemeCatalog themeCatalog;
  private final TenantThemePersistencePort persistencePort;
  private final TenantThemeReaderPort readerPort;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(ApplyTenantThemeCommand cmd) {
    // T3: Validate preset exists and is active
    ThemePresetView preset =
        themeCatalog
            .findByCode(cmd.presetCode())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Theme preset not found or inactive: " + cmd.presetCode()));

    // Check if preset is active
    if (!preset.active()) {
      throw new IllegalArgumentException(
          "Theme preset is not active: " + cmd.presetCode());
    }

    // T1: Persist tenant theme and increment version
    var existing = readerPort.findByTenantId(cmd.tenantId());
    long newVersion = existing.map(t -> t.version() + 1).orElse(1L);

    Instant now = Instant.now(clock);
    var tenantTheme =
        new TenantTheme(
            cmd.tenantId(),
            cmd.presetCode(),
            new HashMap<>(), // metadata can be extended later
            newVersion,
            existing.map(TenantTheme::createdAt).orElse(now),
            now,
            "system" // TODO: get from security context
            );

    var saved = persistencePort.save(tenantTheme);

    // T4: Publish event AFTER COMMIT
    AfterCommit.run(() -> {
      var event =
          new TenantThemeUpdatedEvent(
              saved.tenantId(),
              saved.presetCode(),
              saved.version(),
              saved.updatedAt(),
              saved.createdBy());
      eventPublisher.publishEvent(event);
    });
  }
}
