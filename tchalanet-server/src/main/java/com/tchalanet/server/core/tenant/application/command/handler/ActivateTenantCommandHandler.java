package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenant.application.command.model.ActivateTenantCommand;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.tenant.domain.event.TenantStatusChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ActivateTenantCommandHandler implements VoidCommandHandler<ActivateTenantCommand> {

  private final TenantWriterPort tenantWriterPort;
  private final TenantReaderPort tenantReaderPort;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(ActivateTenantCommand cmd) {
    var t =
        tenantReaderPort
            .findById(cmd.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    var prev = t.status();
    t.activate();
    tenantWriterPort.save(t);

    AfterCommit.run(
        () ->
            publisher.publish(
                new TenantStatusChangedEvent(
                    UUID.randomUUID(),
                    Instant.now(clock),
                    t.id(),
                    prev,
                    t.status(),
                    "activated_by_admin")));
  }
}
