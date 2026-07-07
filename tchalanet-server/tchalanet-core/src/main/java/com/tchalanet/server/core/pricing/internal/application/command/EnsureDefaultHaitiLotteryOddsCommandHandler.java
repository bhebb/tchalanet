package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.command.EnsureDefaultHaitiLotteryOddsCommand;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnsureDefaultHaitiLotteryOddsCommandHandler
    implements CommandHandler<EnsureDefaultHaitiLotteryOddsCommand, Void> {

    private final TenantPricingOddsReaderPort reader;
    private final TenantPricingOddsWriterPort writer;

    @Override
    @TchTx
    public Void handle(EnsureDefaultHaitiLotteryOddsCommand c) {
        if (c.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        for (DefaultPricingOdds row : defaultHaitiLotteryOdds()) {
            ensureOdds(c.tenantId(), row);
        }
        return null;
    }

    private void ensureOdds(TenantId tenantId, DefaultPricingOdds row) {
        var odds = reader.findByNaturalKey(tenantId, row.gameCode(), row.pricingVariantCode())
            .map(existing -> existing.update(row.odds(), row.betType(), row.betOption()))
            .orElseGet(() -> new TenantPricingOdds(
                tenantId,
                row.gameCode(),
                row.pricingVariantCode(),
                row.betType(),
                row.betOption(),
                row.odds(),
                true,
                null));
        writer.save(odds);
    }

    private static List<DefaultPricingOdds> defaultHaitiLotteryOdds() {
        return List.of(
            new DefaultPricingOdds("HT_BOLET", PricingVariantCode.MATCH_1_2D, "MATCH_1_2D", null, "50.0000"),
            new DefaultPricingOdds("HT_BOLET", PricingVariantCode.MATCH_2_2D, "MATCH_2_2D", null, "20.0000"),
            new DefaultPricingOdds("HT_BOLET", PricingVariantCode.MATCH_3_2D, "MATCH_3_2D", null, "10.0000"),
            new DefaultPricingOdds("HT_MARYAJ", PricingVariantCode.MARRIAGE_EXACT_ORDER, "MARRIAGE_2D2D", (short) 1, "1000.0000"),
            new DefaultPricingOdds("HT_MARYAJ", PricingVariantCode.MARRIAGE_REVERSE_ALLOWED, "MARRIAGE_2D2D", (short) 2, "1000.0000"),
            new DefaultPricingOdds("HT_MARYAJ_GRATIS", PricingVariantCode.MARRIAGE_EXACT_ORDER, "MARRIAGE_2D2D", (short) 1, "10.0000"),
            new DefaultPricingOdds("HT_MARYAJ_GRATIS", PricingVariantCode.MARRIAGE_REVERSE_ALLOWED, "MARRIAGE_2D2D", (short) 2, "10.0000"),
            new DefaultPricingOdds("HT_LOTO3", PricingVariantCode.LOTTO3_STRAIGHT, "LOTTO3_3D", (short) 1, "500.0000"),
            new DefaultPricingOdds("HT_LOTO3", PricingVariantCode.LOTTO3_BOX_3_WAY, "LOTTO3_3D", (short) 2, "500.0000"),
            new DefaultPricingOdds("HT_LOTO3", PricingVariantCode.LOTTO3_BOX_6_WAY, "LOTTO3_3D", (short) 2, "500.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_STRAIGHT, "LOTTO4_PATTERN", (short) 1, "5000.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_BOX_4_WAY, "LOTTO4_PATTERN", (short) 2, "1200.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_BOX_6_WAY, "LOTTO4_PATTERN", (short) 2, "800.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_BOX_12_WAY, "LOTTO4_PATTERN", (short) 2, "400.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_BOX_24_WAY, "LOTTO4_PATTERN", (short) 2, "200.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_FRONT_PAIR, "LOTTO4_PATTERN", (short) 3, "5000.0000"),
            new DefaultPricingOdds("HT_LOTO4", PricingVariantCode.LOTTO4_BACK_PAIR, "LOTTO4_PATTERN", (short) 4, "5000.0000"),
            new DefaultPricingOdds("HT_LOTO5", PricingVariantCode.LOTTO5_LOT1_LOT2, "LOTTO5_PATTERN", (short) 1, "25000.0000"),
            new DefaultPricingOdds("HT_LOTO5", PricingVariantCode.LOTTO5_LOT1_LOT3, "LOTTO5_PATTERN", (short) 2, "25000.0000"),
            new DefaultPricingOdds("HT_LOTO5", PricingVariantCode.LOTTO5_MIXED_1_2_3, "LOTTO5_PATTERN", (short) 3, "25000.0000"));
    }

    private record DefaultPricingOdds(
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds
    ) {
        private DefaultPricingOdds(
            String gameCode,
            PricingVariantCode pricingVariantCode,
            String betType,
            Short betOption,
            String odds
        ) {
            this(gameCode, pricingVariantCode, betType, betOption, new BigDecimal(odds));
        }
    }
}
