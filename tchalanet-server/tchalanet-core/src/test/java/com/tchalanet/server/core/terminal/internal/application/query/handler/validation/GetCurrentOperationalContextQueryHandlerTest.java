package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.api.query.GetCurrentOperationalContextQuery;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCurrentOperationalContextQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));

    @Test
    void returnsNoContextWhenRequestHasNoOperationalFrame() {
        var result = new GetCurrentOperationalContextQueryHandler()
            .handle(new GetCurrentOperationalContextQuery(TENANT_ID, USER_ID, null));

        assertThat(result.present()).isFalse();
        assertThat(result.source()).isEqualTo(OperationalContextSource.NONE);
        assertThat(result.trustedForSensitiveOperation()).isFalse();
    }

    @Test
    void returnsTrustedContextSnapshotWithoutSecrets() {
        var result = new GetCurrentOperationalContextQueryHandler()
            .handle(new GetCurrentOperationalContextQuery(
                TENANT_ID,
                USER_ID,
                new OperationalContextHint(
                    TERMINAL_ID,
                    OUTLET_ID,
                    null,
                    OperationalContextSource.SIGNED_DEVICE_BINDING,
                    OperationalContextTrust.STRONG)));

        assertThat(result.present()).isTrue();
        assertThat(result.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(result.outletId()).isEqualTo(OUTLET_ID);
        assertThat(result.source()).isEqualTo(OperationalContextSource.SIGNED_DEVICE_BINDING);
        assertThat(result.trustedForSensitiveOperation()).isTrue();
    }
}
