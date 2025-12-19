package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenant.application.command.model.CreateTenantCommand;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.tenant.domain.event.TenantCreatedEvent;
import com.tchalanet.server.core.tenant.domain.model.Tenant;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class CreateTenantCommandHandler implements CommandHandler<CreateTenantCommand, UUID> {

    private final TenantWriterPort tenantWriterPort;
    private final TenantReaderPort tenantReaderPort;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public UUID handle(CreateTenantCommand cmd) {
        var codeLower = cmd.code().trim().toLowerCase();
        if (tenantReaderPort.existsByCode(codeLower)) {
            throw new IllegalArgumentException("Tenant code already exists: " + codeLower);
        }

        var tenant = Tenant.createDraft(
            new TenantId(UUID.randomUUID()),
            codeLower,
            cmd.name(),
            cmd.type(),
            cmd.timezone(),
            cmd.currency()
        );

        var saved = tenantWriterPort.save(tenant);

        var evt = new TenantCreatedEvent(
            UUID.randomUUID(),
            Instant.now(clock),
            saved.id(),
            saved.code()
        );
        AfterCommit.run(() -> publisher.publish(evt));

        return saved.id().value();
    }
}
