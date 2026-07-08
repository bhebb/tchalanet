package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.command.DeleteTenantOddsCommand;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteTenantOddsCommandHandler implements CommandHandler<DeleteTenantOddsCommand, Void> {

    private final TenantPricingOddsReaderPort reader;
    private final TenantPricingOddsWriterPort writer;

    @Override
    @TchTx
    public Void handle(DeleteTenantOddsCommand c) {
        var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
        var odds = reader.findByNaturalKey(c.tenantId(), gameCode, c.pricingVariantCode())
            .orElse(null);
        if (odds != null && odds.active()) {
            writer.save(odds.softDelete());
        }
        return null;
    }
}
