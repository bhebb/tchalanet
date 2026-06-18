package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.GetCurrentOperationalContextQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCurrentOperationalContextQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final SellerTerminalId SELLER_TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    @Test
    void returnsNoContextWhenRequestHasNoSellerTerminal() {
        var result = new GetCurrentOperationalContextQueryHandler(reader())
            .handle(new GetCurrentOperationalContextQuery(
                TENANT_ID,
                null,
                null,
                null,
                false));

        assertThat(result.present()).isFalse();
        assertThat(result.sellerTerminalId()).isNull();
        assertThat(result.source()).isEqualTo(OperationalContextSource.NONE);
        assertThat(result.trustedForSensitiveOperation()).isFalse();
    }

    @Test
    void returnsSellerTerminalContextSnapshotWithoutLegacyOperationalIds() {
        var result = new GetCurrentOperationalContextQueryHandler(reader())
            .handle(new GetCurrentOperationalContextQuery(
                TENANT_ID,
                SELLER_TERMINAL_ID,
                OperationalContextSource.SIGNED_DEVICE_BINDING,
                OperationalContextTrust.STRONG,
                true));

        assertThat(result.present()).isTrue();
        assertThat(result.sellerTerminalId()).isEqualTo(SELLER_TERMINAL_ID);
        assertThat(result.terminalCode()).isEqualTo("ST-001");
        assertThat(result.displayName()).isEqualTo("Seller terminal 001");
        assertThat(result.status()).isEqualTo(SellerTerminalStatus.ACTIVE);
        assertThat(result.source()).isEqualTo(OperationalContextSource.SIGNED_DEVICE_BINDING);
        assertThat(result.trust()).isEqualTo(OperationalContextTrust.STRONG);
        assertThat(result.trustedForSensitiveOperation()).isTrue();
    }

    private static SellerTerminalReaderPort reader() {
        return new SellerTerminalReaderPort() {
            @Override
            public Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId id) {
                if (!TENANT_ID.equals(tenantId) || !SELLER_TERMINAL_ID.equals(id)) {
                    return Optional.empty();
                }
                return Optional.of(SellerTerminal.createPending(
                    SELLER_TERMINAL_ID,
                    TENANT_ID,
                    "ST-001",
                    "Seller terminal 001",
                    "Seller",
                    "Terminal",
                    "+50900000000",
                    (AddressId) null,
                    new BigDecimal("15.00")).activate(java.time.Instant.EPOCH));
            }

            @Override
            public Optional<SellerTerminal> findByExternalSubject(String provider, String issuer, String externalSubject) {
                return Optional.empty();
            }

            @Override
            public TchPage<SellerTerminalSummaryRow> search(
                TenantId tenantId,
                SellerTerminalSearchCriteria criteria,
                TchPageRequest pageRequest
            ) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public SellerTerminalCommissionStatsView commissionStats(TenantId tenantId, BigDecimal tenantDefaultRate) {
                throw new UnsupportedOperationException("not used");
            }
        };
    }
}
