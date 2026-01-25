package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenantconfig.application.command.model.ActivateTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantStatusChangedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Command handler: Activate Tenant (DRAFT → ACTIVE).
 * Per DOMAIN_TENANT_CONFIG.md + command_query_handlers.md + core.tenant pattern:
 * - Implements VoidCommandHandler<ActivateTenantCommand>
 * - Uses TenantCatalog to read TenantRegistryView
 * - Converts to TenantConfig domain model via factory method
 * - Validate current status
 * - Transition to ACTIVE
 * - Publish TenantStatusChangedEvent with reason
 */
@UseCase
@RequiredArgsConstructor
public class ActivateTenantCommandHandler implements VoidCommandHandler<ActivateTenantCommand> {

  private final TenantCatalog tenantCatalog;
  private final TenantConfigWriterPort writer;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(ActivateTenantCommand cmd) {
    // Read tenant registry view from catalog
    var registryView = tenantCatalog.findRegistryById(cmd.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + cmd.tenantId()));

    // Convert to domain model for mutation (no mapper needed!)
    var tenant = TenantConfig.fromRegistryView(registryView);

    var prevStatus = tenant.status();
    var now = Instant.now(clock);
    var activated = tenant.activate(now);

    var saved = writer.update(activated);

    // Publish event after commit (only if status changed)
    if (saved.status() != prevStatus) {
      var evt = new TenantStatusChangedEvent(
          UUID.randomUUID(),
          now,
          saved.id(),
          prevStatus,
          saved.status(),
          "activated_by_admin"  // per core.tenant pattern
      );
      AfterCommit.run(() -> publisher.publish(evt));
    }
  }
}
