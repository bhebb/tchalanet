package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalOddsOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalOddsOverrideResult;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertSellerTerminalOddsOverrideCommandHandler
    implements CommandHandler<UpsertSellerTerminalOddsOverrideCommand, UpsertSellerTerminalOddsOverrideResult> {

    private final SellerTerminalOddsOverrideReaderPort reader;
    private final SellerTerminalOddsOverrideWriterPort writer;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public UpsertSellerTerminalOddsOverrideResult handle(UpsertSellerTerminalOddsOverrideCommand c) {
        validate(c);

        var existing = reader.findActiveByNaturalKey(
            c.tenantId(), c.sellerTerminalId(),
            c.gameCode(), c.betType(), c.betOption());

        if (existing.isPresent()) {
            var updated = existing.get().update(
                c.odds(), c.effectiveFrom(), c.effectiveTo(), c.reason(), c.actorId());
            var saved = writer.save(updated);
            return new UpsertSellerTerminalOddsOverrideResult(saved.id(), false);
        }

        var created = SellerTerminalOddsOverride.createNew(
            SellerTerminalOddsOverrideId.of(idGenerator.newUuid()),
            c.tenantId(), c.sellerTerminalId(),
            c.gameCode(), c.betType(), c.betOption(),
            c.odds(), c.effectiveFrom(), c.effectiveTo(), c.reason(), c.actorId());
        var saved = writer.save(created);
        return new UpsertSellerTerminalOddsOverrideResult(saved.id(), true);
    }

    private void validate(UpsertSellerTerminalOddsOverrideCommand c) {
        if (c.odds() == null || c.odds().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("odds must be positive");
        }
        if (c.gameCode() == null || c.gameCode().isBlank()) {
            throw new IllegalArgumentException("gameCode is required");
        }
        if (c.betType() == null || c.betType().isBlank()) {
            throw new IllegalArgumentException("betType is required");
        }
    }
}
