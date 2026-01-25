package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenantconfig.application.command.model.ArchiveTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantStatusChangedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class ArchiveTenantCommandHandler implements VoidCommandHandler<ArchiveTenantCommand> {

  private final TenantCatalog tenantCatalog;
  private final TenantConfigWriterPort writer;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(ArchiveTenantCommand cmd) {
    var registryView = tenantCatalog.findRegistryById(cmd.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + cmd.tenantId()));

    var tenant = TenantConfig.fromRegistryView(registryView);
    var prevStatus = tenant.status();
    var now = Instant.now(clock);
    var archived = tenant.archive(now);
    var saved = writer.update(archived);

    if (saved.status() != prevStatus) {
      var reason = cmd.reason() == null ? "archived_by_admin" : cmd.reason();
      var evt = new TenantStatusChangedEvent(
          UUID.randomUUID(), now, saved.id(), prevStatus, saved.status(), reason);
      AfterCommit.run(() -> publisher.publish(evt));
    }
  }
}
