package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenant.application.command.model.ArchiveTenantCommand;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.tenant.domain.event.TenantStatusChangedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
class ArchiveTenantCommandHandler implements VoidCommandHandler<ArchiveTenantCommand> {

    private final TenantWriterPort tenantWriterPort;
    private final TenantReaderPort tenantReaderPort;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(ArchiveTenantCommand cmd) {
        var t = tenantReaderPort.findById(cmd.tenantId()).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        var prev = t.status();
        t.archive();
        tenantWriterPort.save(t);

        String reason = cmd.reason() == null ? "archived_by_admin" : cmd.reason();
        AfterCommit.run(() -> publisher.publish(new TenantStatusChangedEvent(
            UUID.randomUUID(), Instant.now(clock), t.id(), prev, t.status(), reason
        )));
    }
}
