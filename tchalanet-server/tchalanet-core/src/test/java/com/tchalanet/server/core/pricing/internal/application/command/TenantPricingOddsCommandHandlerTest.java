package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.command.DeleteTenantOddsCommand;
import com.tchalanet.server.core.pricing.api.command.EnsureDefaultHaitiLotteryOddsCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantOddsCommand;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantPricingOddsCommandHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    private final InMemoryTenantPricingOddsStore store = new InMemoryTenantPricingOddsStore();
    private final TenantPricingOddsMapper mapper = new TenantPricingOddsMapper();

    @Test
    void upsertCreatesTenantOddsByPricingVariant() {
        var handler = new UpsertTenantOddsCommandHandler(store, store, mapper);

        var result = handler.handle(new UpsertTenantOddsCommand(
            TENANT_ID,
            "ht_loto4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "lotto4_pattern",
            (short) 2,
            new BigDecimal("1200.0000"),
            null
        ));

        assertThat(result.gameCode()).isEqualTo("HT_LOTO4");
        assertThat(result.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO4_BOX_4_WAY);
        assertThat(result.betType()).isEqualTo("LOTTO4_PATTERN");
        assertThat(result.odds()).isEqualByComparingTo("1200.0000");
        assertThat(result.active()).isTrue();
    }

    @Test
    void upsertReactivatesDeletedTenantOddsWithoutCreatingDuplicateNaturalKey() {
        var handler = new UpsertTenantOddsCommandHandler(store, store, mapper);
        store.saved.add(new TenantPricingOdds(
            TENANT_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            new BigDecimal("1200.0000"),
            false,
            null
        ));

        handler.handle(new UpsertTenantOddsCommand(
            TENANT_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            new BigDecimal("1300.0000"),
            null
        ));

        assertThat(store.saved).hasSize(1);
        assertThat(store.saved.getFirst().active()).isTrue();
        assertThat(store.saved.getFirst().odds()).isEqualByComparingTo("1300.0000");
    }

    @Test
    void deleteDisablesTenantOddsButKeepsNaturalKeyReusable() {
        var handler = new DeleteTenantOddsCommandHandler(store, store);
        store.saved.add(new TenantPricingOdds(
            TENANT_ID,
            "HT_LOTO3",
            PricingVariantCode.LOTTO3_BOX_6_WAY,
            "LOTTO3_3D",
            (short) 2,
            new BigDecimal("500.0000"),
            true,
            null
        ));

        handler.handle(new DeleteTenantOddsCommand(
            TENANT_ID,
            "HT_LOTO3",
            PricingVariantCode.LOTTO3_BOX_6_WAY,
            null
        ));

        assertThat(store.saved).hasSize(1);
        assertThat(store.saved.getFirst().active()).isFalse();
        assertThat(store.saved.getFirst().deletedAt()).isNull();
    }

    @Test
    void ensureDefaultHaitiLotteryOddsSeedsVariantKeyedTenantDefaultsIdempotently() {
        var handler = new EnsureDefaultHaitiLotteryOddsCommandHandler(store, store);

        handler.handle(new EnsureDefaultHaitiLotteryOddsCommand(TENANT_ID));
        handler.handle(new EnsureDefaultHaitiLotteryOddsCommand(TENANT_ID));

        assertThat(store.saved).hasSize(20);
        assertThat(store.findByNaturalKey(TENANT_ID, "HT_LOTO4", PricingVariantCode.LOTTO4_BOX_4_WAY))
            .get()
            .extracting(TenantPricingOdds::odds)
            .satisfies(odds -> assertThat(odds).isEqualByComparingTo("1200.0000"));
        assertThat(store.findByNaturalKey(TENANT_ID, "HT_LOTO4", PricingVariantCode.LOTTO4_BOX_24_WAY))
            .get()
            .extracting(TenantPricingOdds::odds)
            .satisfies(odds -> assertThat(odds).isEqualByComparingTo("200.0000"));
    }

    private static final class InMemoryTenantPricingOddsStore
        implements TenantPricingOddsReaderPort, TenantPricingOddsWriterPort {

        private final List<TenantPricingOdds> saved = new ArrayList<>();

        @Override
        public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
            return saved.stream()
                .filter(TenantPricingOdds::active)
                .filter(odds -> odds.tenantId().equals(tenantId))
                .filter(odds -> gameCode == null || odds.gameCode().equals(gameCode))
                .toList();
        }

        @Override
        public Optional<TenantPricingOdds> findByNaturalKey(
            TenantId tenantId,
            String gameCode,
            PricingVariantCode pricingVariantCode
        ) {
            return saved.stream()
                .filter(odds -> odds.tenantId().equals(tenantId))
                .filter(odds -> odds.gameCode().equals(gameCode))
                .filter(odds -> odds.pricingVariantCode() == pricingVariantCode)
                .findFirst();
        }

        @Override
        public TenantPricingOdds save(TenantPricingOdds odds) {
            findByNaturalKey(odds.tenantId(), odds.gameCode(), odds.pricingVariantCode())
                .ifPresent(saved::remove);
            saved.add(odds);
            return odds;
        }
    }
}
