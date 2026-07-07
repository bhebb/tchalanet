package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantOddsCommand;
import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertTenantOddsCommandHandler implements CommandHandler<UpsertTenantOddsCommand, TenantPricingOddsView> {

    private final TenantPricingOddsReaderPort reader;
    private final TenantPricingOddsWriterPort writer;
    private final TenantPricingOddsMapper mapper;

    @Override
    @TchTx
    public TenantPricingOddsView handle(UpsertTenantOddsCommand c) {
        var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
        var betType = TenantPricingOdds.normalizeBetType(c.betType());

        var odds = reader.findByNaturalKey(c.tenantId(), gameCode, c.pricingVariantCode())
            .map(existing -> existing.update(c.odds(), betType, c.betOption()))
            .orElseGet(() -> new TenantPricingOdds(
                c.tenantId(),
                gameCode,
                c.pricingVariantCode(),
                betType,
                c.betOption(),
                c.odds(),
                true,
                null
            ));

        return mapper.toView(writer.save(odds));
    }
}
