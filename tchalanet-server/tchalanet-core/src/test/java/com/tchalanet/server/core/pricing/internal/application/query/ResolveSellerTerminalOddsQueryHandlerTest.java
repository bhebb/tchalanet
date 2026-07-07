package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolveSellerTerminalOddsQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final SellerTerminalId SELLER_TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

    @Test
    void resolvesTenantDefaultByPricingVariantCode() {
        var tenantOddsReader = new CapturingTenantOddsReader(new BigDecimal("1200.0000"));
        var handler = new ResolveSellerTerminalOddsQueryHandler(new NoOverrideReader(), tenantOddsReader);

        var result = handler.handle(new ResolveSellerTerminalOddsQuery(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2));

        assertThat(tenantOddsReader.capturedVariantCode).isEqualTo(PricingVariantCode.LOTTO4_BOX_4_WAY);
        assertThat(result.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO4_BOX_4_WAY);
        assertThat(result.tenantDefaultOdds()).isEqualByComparingTo("1200.0000");
        assertThat(result.effectiveOdds()).isEqualByComparingTo("1200.0000");
        assertThat(result.source()).isEqualTo(OddsSource.TENANT_DEFAULT);
    }

    @Test
    void resolvesSellerTerminalOverrideByPricingVariantCode() {
        var tenantOddsReader = new CapturingTenantOddsReader(new BigDecimal("1200.0000"));
        var overrideReader = new MatchingOverrideReader(PricingVariantCode.LOTTO4_BOX_4_WAY, new BigDecimal("900.0000"));
        var handler = new ResolveSellerTerminalOddsQueryHandler(overrideReader, tenantOddsReader);

        var result = handler.handle(new ResolveSellerTerminalOddsQuery(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2));

        assertThat(result.tenantDefaultOdds()).isEqualByComparingTo("1200.0000");
        assertThat(result.sellerTerminalOdds()).isEqualByComparingTo("900.0000");
        assertThat(result.effectiveOdds()).isEqualByComparingTo("900.0000");
        assertThat(result.source()).isEqualTo(OddsSource.SELLER_TERMINAL_OVERRIDE);
    }

    @Test
    void rejectsMissingTenantDefaultOdds() {
        var tenantOddsReader = new MissingTenantOddsReader();
        var handler = new ResolveSellerTerminalOddsQueryHandler(new NoOverrideReader(), tenantOddsReader);

        assertThatThrownBy(() -> handler.handle(new ResolveSellerTerminalOddsQuery(
                TENANT_ID,
                SELLER_TERMINAL_ID,
                "HT_LOTO4",
                PricingVariantCode.LOTTO4_BOX_4_WAY,
                "LOTTO4_PATTERN",
                (short) 2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tenant default pricing odds missing")
            .hasMessageContaining("HT_LOTO4")
            .hasMessageContaining("LOTTO4_BOX_4_WAY");
    }

    private static final class CapturingTenantOddsReader implements TenantPricingOddsReaderPort {
        private final BigDecimal odds;
        private PricingVariantCode capturedVariantCode;

        private CapturingTenantOddsReader(BigDecimal odds) {
            this.odds = odds;
        }

        @Override
        public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
            return List.of();
        }

        @Override
        public Optional<TenantPricingOdds> findByNaturalKey(
            TenantId tenantId,
            String gameCode,
            PricingVariantCode pricingVariantCode
        ) {
            this.capturedVariantCode = pricingVariantCode;
            return Optional.of(new TenantPricingOdds(
                tenantId,
                gameCode,
                pricingVariantCode,
                "LOTTO4_PATTERN",
                (short) 2,
                odds,
                true,
                null));
        }
    }

    private static final class NoOverrideReader implements SellerTerminalOddsOverrideReaderPort {
        @Override
        public Optional<SellerTerminalOddsOverride> findById(com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId id) {
            return Optional.empty();
        }

        @Override
        public List<SellerTerminalOddsOverride> findActiveBySellerTerminal(TenantId tenantId, SellerTerminalId sellerTerminalId) {
            return List.of();
        }

        @Override
        public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
            TenantId tenantId,
            SellerTerminalId sellerTerminalId,
            String gameCode,
            PricingVariantCode pricingVariantCode
        ) {
            return Optional.empty();
        }
    }

    private static final class MatchingOverrideReader implements SellerTerminalOddsOverrideReaderPort {
        private final PricingVariantCode pricingVariantCode;
        private final BigDecimal odds;

        private MatchingOverrideReader(PricingVariantCode pricingVariantCode, BigDecimal odds) {
            this.pricingVariantCode = pricingVariantCode;
            this.odds = odds;
        }

        @Override
        public Optional<SellerTerminalOddsOverride> findById(com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId id) {
            return Optional.empty();
        }

        @Override
        public List<SellerTerminalOddsOverride> findActiveBySellerTerminal(TenantId tenantId, SellerTerminalId sellerTerminalId) {
            return List.of();
        }

        @Override
        public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
            TenantId tenantId,
            SellerTerminalId sellerTerminalId,
            String gameCode,
            PricingVariantCode pricingVariantCode
        ) {
            if (this.pricingVariantCode != pricingVariantCode) {
                return Optional.empty();
            }
            return Optional.of(SellerTerminalOddsOverride.createNew(
                com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId.of(
                    UUID.fromString("40000000-0000-0000-0000-000000000001")),
                tenantId,
                sellerTerminalId,
                gameCode,
                pricingVariantCode,
                "LOTTO4_PATTERN",
                (short) 2,
                odds,
                null,
                null,
                null,
                null));
        }
    }

    private static final class MissingTenantOddsReader implements TenantPricingOddsReaderPort {
        @Override
        public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
            return List.of();
        }

        @Override
        public Optional<TenantPricingOdds> findByNaturalKey(
            TenantId tenantId,
            String gameCode,
            PricingVariantCode pricingVariantCode
        ) {
            return Optional.empty();
        }
    }
}
