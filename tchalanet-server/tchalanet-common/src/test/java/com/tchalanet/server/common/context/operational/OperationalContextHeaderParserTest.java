package com.tchalanet.server.common.context.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.error.ProblemRestException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationalContextHeaderParserTest {

    private static final String TERMINAL_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OUTLET_ID = "22222222-2222-2222-2222-222222222222";
    private static final String SALES_SESSION_ID = "33333333-3333-3333-3333-333333333333";

    @Test
    void noHeadersReturnsNoContext() {
        var hint = OperationalContextHeaderParser.parseHint(Map.<String, String>of()::get);

        assertThat(hint.hasPosFrame()).isFalse();
        assertThat(hint.source()).isEqualTo(OperationalContextSource.NONE);
        assertThat(hint.trust()).isEqualTo(OperationalContextTrust.NONE);
    }

    @Test
    void clientClaimHeadersCreateWeakHint() {
        var hint = OperationalContextHeaderParser.parseHint(headers(
            OperationalContextHeaders.TERMINAL_ID, TERMINAL_ID,
            OperationalContextHeaders.OUTLET_ID, OUTLET_ID,
            OperationalContextHeaders.SALES_SESSION_ID, SALES_SESSION_ID));

        assertThat(hint.hasPosFrame()).isTrue();
        assertThat(hint.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(hint.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    @Test
    void operationalTrustHeaderDoesNotAffectDerivedTrust() {
        var hint = OperationalContextHeaderParser.parseHint(headers(
            OperationalContextHeaders.TERMINAL_ID, TERMINAL_ID,
            OperationalContextHeaders.OUTLET_ID, OUTLET_ID,
            OperationalContextHeaders.SALES_SESSION_ID, SALES_SESSION_ID,
            "X-Tch-Operational-Trust", "STRONG"));

        assertThat(hint.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(hint.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    @Test
    void adminSelectionWithoutServerTokenDowngradesToWeakClientClaim() {
        var hint = OperationalContextHeaderParser.parseHint(headers(
            OperationalContextHeaders.TERMINAL_ID, TERMINAL_ID,
            OperationalContextHeaders.OUTLET_ID, OUTLET_ID,
            OperationalContextHeaders.SALES_SESSION_ID, SALES_SESSION_ID,
            OperationalContextHeaders.OPERATIONAL_SOURCE, "ADMIN_SELECTION"));

        assertThat(hint.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(hint.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    @Test
    void signedDeviceBindingWithoutProofIsRejected() {
        assertThatThrownBy(() -> OperationalContextHeaderParser.parseHint(headers(
            OperationalContextHeaders.TERMINAL_ID, TERMINAL_ID,
            OperationalContextHeaders.OUTLET_ID, OUTLET_ID,
            OperationalContextHeaders.SALES_SESSION_ID, SALES_SESSION_ID,
            OperationalContextHeaders.OPERATIONAL_SOURCE, "SIGNED_DEVICE_BINDING")))
            .isInstanceOf(ProblemRestException.class);
    }

    private static OperationalContextHeaderParser.HeaderReader headers(String... pairs) {
        var values = new java.util.HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values::get;
    }
}
