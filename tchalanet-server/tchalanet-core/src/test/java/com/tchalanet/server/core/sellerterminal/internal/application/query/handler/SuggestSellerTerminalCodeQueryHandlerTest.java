package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.api.query.SuggestSellerTerminalCodeQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuggestSellerTerminalCodeQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    @Test
    void startsAtPos001WhenTenantHasNoPosCodes() {
        var result = handler(List.of()).handle(new SuggestSellerTerminalCodeQuery(TENANT_ID));

        assertThat(result.terminalCode()).isEqualTo("POS-001");
    }

    @Test
    void incrementsLargestExistingPosCodeAndIgnoresLegacyCodes() {
        var result = handler(List.of("TCH-ABCD12", "POS-001", "pos-003", "STORE-9", "POS-999999999999"))
            .handle(new SuggestSellerTerminalCodeQuery(TENANT_ID));

        assertThat(result.terminalCode()).isEqualTo("POS-004");
    }

    @Test
    void keepsGrowingAfterThreeDigits() {
        var result = handler(List.of("POS-998", "POS-999"))
            .handle(new SuggestSellerTerminalCodeQuery(TENANT_ID));

        assertThat(result.terminalCode()).isEqualTo("POS-1000");
    }

    private static SuggestSellerTerminalCodeQueryHandler handler(List<String> codes) {
        return new SuggestSellerTerminalCodeQueryHandler(reader(codes));
    }

    private static SellerTerminalReaderPort reader(List<String> codes) {
        return new SellerTerminalReaderPort() {
            @Override
            public Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId id) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public Optional<SellerTerminal> findByExternalSubject(String provider, String issuer, String externalSubject) {
                throw new UnsupportedOperationException("not used");
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
            public List<String> terminalCodes(TenantId tenantId) {
                return codes;
            }

            @Override
            public SellerTerminalCommissionStatsView commissionStats(TenantId tenantId, BigDecimal tenantDefaultRate) {
                throw new UnsupportedOperationException("not used");
            }
        };
    }
}
