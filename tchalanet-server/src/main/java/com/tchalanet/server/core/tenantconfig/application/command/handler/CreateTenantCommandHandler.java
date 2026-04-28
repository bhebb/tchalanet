package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.tenantconfig.application.command.model.CreateTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantStatusChangedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateTenantCommandHandler implements VoidCommandHandler<CreateTenantCommand> {

  private final TenantConfigWriterPort writer;
  private final AddressCrudService addressCrudService;
  private final DomainEventPublisher publisher;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public void handle(CreateTenantCommand cmd) {
    var now = Instant.now(clock);
    var newTenantId = TenantId.of(idGenerator.newUuid());
    var addressId =
        cmd.address() == null ? null : addressCrudService.upsertTenantPrimary(newTenantId, cmd.address());

    TenantConfig tenant =
        TenantConfig.createDraft(
            newTenantId,
            cmd.code(),
            cmd.name(),
            cmd.type(),
            cmd.timezone(),
            cmd.currency(),
            addressId,
            cmd.activeThemeId());

    if (Boolean.TRUE.equals(cmd.activate())) {
      tenant = tenant.activate(now);
    }

    var saved = writer.create(tenant);
    var fromStatus = Boolean.TRUE.equals(cmd.activate()) ? com.tchalanet.server.common.types.enums.TenantStatus.DRAFT : null;
    var reason = Boolean.TRUE.equals(cmd.activate()) ? "activated_on_create" : "tenant_created";

    var evt =
        new TenantStatusChangedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            saved.id(),
            fromStatus,
            saved.status(),
            reason);
    AfterCommit.run(() -> publisher.publish(evt));
  }
}
