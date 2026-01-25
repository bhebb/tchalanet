package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.core.tenantconfig.application.command.model.DeactivateTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantStatusChangedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Command handler: Deactivate Tenant (ACTIVE → SUSPENDED).
 * Per DOMAIN_TENANT_CONFIG.md + command_query_handlers.md + core.tenant pattern:
 * - Implements VoidCommandHandler<DeactivateTenantCommand>
 * - Alias for Suspend with explicit naming per core.tenant
 * - Validate current status is ACTIVE
 * - Transition to SUSPENDED
 * - Includes reason for audit trail
 * - Publish TenantStatusChangedEvent with reason
 */
@UseCase
@RequiredArgsConstructor
public class DeactivateTenantCommandHandler implements VoidCommandHandler<DeactivateTenantCommand> {

  private final TenantCatalog tenantCatalog;
  private final TenantConfigWriterPort writer;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(DeactivateTenantCommand cmd) {
    var registryView = tenantCatalog.findRegistryById(cmd.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + cmd.tenantId()));

    var tenant = TenantConfig.fromRegistryView(registryView);
    var prevStatus = tenant.status();
    var now = Instant.now(clock);
    var suspended = tenant.suspend(now);
    var saved = writer.update(suspended);

    // Publish event after commit (only if status changed)
    if (saved.status() != prevStatus) {
      var reason = cmd.reason() == null ? "deactivated_by_admin" : cmd.reason();
      var evt = new TenantStatusChangedEvent(
          UUID.randomUUID(), now, saved.id(), prevStatus, saved.status(), reason);
      AfterCommit.run(() -> publisher.publish(evt));
    }
  }
}
