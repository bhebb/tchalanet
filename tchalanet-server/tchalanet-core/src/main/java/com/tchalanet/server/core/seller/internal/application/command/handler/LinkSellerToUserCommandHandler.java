package com.tchalanet.server.core.seller.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.seller.api.command.LinkSellerToUserCommand;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.event.SellerLinkedToUserEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class LinkSellerToUserCommandHandler implements CommandHandler<LinkSellerToUserCommand, Void> {

    private final SellerReaderPort reader;
    private final SellerWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public Void handle(LinkSellerToUserCommand command) {
        var now = Instant.now(clock);
        var seller = reader.getSellerRequired(command.tenantId(), command.sellerId());
        var updated = seller.withUserId(command.userId(), now);
        writer.saveSeller(updated);

        AfterCommit.run(() -> publisher.publish(new SellerLinkedToUserEvent(
            EventId.of(idGenerator.newUuid()), now, updated.tenantId(),
            updated.id(), command.userId())));

        return null;
    }
}
