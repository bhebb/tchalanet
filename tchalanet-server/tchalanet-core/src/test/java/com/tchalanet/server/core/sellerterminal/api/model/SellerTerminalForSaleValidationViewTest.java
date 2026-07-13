package com.tchalanet.server.core.sellerterminal.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerTerminalForSaleValidationViewTest {

    private static final SellerTerminalId TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    @Test
    void activeTerminalCanSellOnlyWhenPinDoesNotNeedChange() {
        assertThat(view(SellerTerminalStatus.ACTIVE, false).canSell()).isTrue();
        assertThat(view(SellerTerminalStatus.ACTIVE, true).canSell()).isFalse();
    }

    @Test
    void activeTerminalWithPinChangeCanSellWhenActorDoesNotRequirePinChangeCompletion() {
        var view = view(SellerTerminalStatus.ACTIVE, true);

        assertThat(view.canSell(true)).isFalse();
        assertThat(view.canSell(false)).isTrue();
    }

    @Test
    void nonActiveTerminalCannotSell() {
        assertThat(view(SellerTerminalStatus.PENDING, false).canSell()).isFalse();
        assertThat(view(SellerTerminalStatus.BLOCKED, false).canSell()).isFalse();
        assertThat(view(SellerTerminalStatus.DISABLED, false).canSell()).isFalse();
    }

    private static SellerTerminalForSaleValidationView view(
        SellerTerminalStatus status,
        boolean mustChangePin
    ) {
        return new SellerTerminalForSaleValidationView(
            TERMINAL_ID,
            TENANT_ID,
            status,
            BigDecimal.TEN,
            mustChangePin);
    }
}
