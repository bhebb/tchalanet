package com.tchalanet.server.core.seller.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.core.seller.api.command.CreateSellerCommand;
import com.tchalanet.server.core.seller.api.model.SellerStatus;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.event.SellerCreatedEvent;
import com.tchalanet.server.core.seller.internal.domain.model.Seller;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CreateSellerCommandHandler implements CommandHandler<CreateSellerCommand, SellerId> {

    private final SellerReaderPort reader;
    private final SellerWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public SellerId handle(CreateSellerCommand command) {
        // Idempotent: if a seller already exists for this userId, return the existing id.
        if (command.userId() != null) {
            var existing = reader.findSellerByUserId(command.tenantId(), command.userId());
            if (existing.isPresent()) {
                return existing.get().id();
            }
        }

        var now = Instant.now(clock);
        var sellerId = SellerId.of(idGenerator.newUuid());

        var seller = Seller.create(
            sellerId,
            command.tenantId(),
            command.userId(),
            command.code() == null ? null : command.code().trim(),
            command.displayName().trim(),
            SellerStatus.ACTIVE,
            now
        );

        var saved = writer.saveSeller(seller);

        AfterCommit.run(() -> publisher.publish(new SellerCreatedEvent(
            EventId.of(idGenerator.newUuid()), now, saved.tenantId(),
            saved.id(), saved.userId(), saved.displayName())));

        return saved.id();
    }
}
