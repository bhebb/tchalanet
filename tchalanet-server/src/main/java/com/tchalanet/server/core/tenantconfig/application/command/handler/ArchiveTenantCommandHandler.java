package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.tenantconfig.application.command.model.ArchiveTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantStatusChangedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ArchiveTenantCommandHandler implements VoidCommandHandler<ArchiveTenantCommand> {

  private final TenantCatalog tenantCatalog;
  private final TenantConfigWriterPort writer;
  private final DomainEventPublisher publisher;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public void handle(ArchiveTenantCommand cmd) {
    var registryView =
        tenantCatalog
            .findRegistryById(cmd.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + cmd.tenantId()));

    var tenant = TenantConfig.fromRegistryView(registryView);
    var prevStatus = tenant.status();
    var now = Instant.now(clock);
    var archived = tenant.archive(now);

    var saved = writer.update(archived);

    if (saved.status() != prevStatus) {
      var evt =
          new TenantStatusChangedEvent(
              EventId.of(idGenerator.newUuid()),
              now,
              saved.id(),
              prevStatus,
              saved.status(),
              "archived_by_admin");
      AfterCommit.run(() -> publisher.publish(evt));
    }
  }
}
