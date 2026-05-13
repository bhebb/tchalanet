package com.tchalanet.server.common.context.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.context.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextHeaders;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationalContextHeaderParserTest {

    private final OperationalContextHeaderParser parser = new OperationalContextHeaderParser();

    @Test
    void noHeadersReturnsEmptyContext() {
        var context = parser.parseBridge(Map.<String, String>of()::get);

        assertThat(context.source()).isEqualTo(OperationalContextSource.NONE);
        assertThat(context.hasTerminal()).isFalse();
        assertThat(context.hasOutlet()).isFalse();
        assertThat(context.hasSalesSession()).isFalse();
    }

    @Test
    void clientClaimDefaultsToWeakWhenPosHeadersArePresentWithoutSource() {
        var context = parser.parseBridge(headers(
            OperationalContextHeaders.TERMINAL_ID, "00000000-0000-0000-0000-000000000001",
            OperationalContextHeaders.OUTLET_ID, "00000000-0000-0000-0000-000000000002",
            OperationalContextHeaders.SALES_SESSION_ID, "00000000-0000-0000-0000-000000000003")::get);

        assertThat(context.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(context.isTrustedForSensitiveOperation()).isFalse();
    }

    @Test
    void trustedSourceHeaderMapsToStrongBridgeSource() {
        var context = parser.parseBridge(headers(
            OperationalContextHeaders.TERMINAL_ID, "00000000-0000-0000-0000-000000000001",
            OperationalContextHeaders.OUTLET_ID, "00000000-0000-0000-0000-000000000002",
            OperationalContextHeaders.SALES_SESSION_ID, "00000000-0000-0000-0000-000000000003",
            OperationalContextHeaders.OPERATIONAL_SOURCE, "signed_device_binding")::get);

        assertThat(context.source()).isEqualTo(OperationalContextSource.SIGNED_DEVICE_BINDING);
        assertThat(context.isTrustedForSensitiveOperation()).isTrue();
    }

    @Test
    void blankHeadersAreIgnored() {
        var context = parser.parseBridge(headers(
            OperationalContextHeaders.TERMINAL_ID, " ",
            OperationalContextHeaders.OUTLET_ID, "\t",
            OperationalContextHeaders.SALES_SESSION_ID, "")::get);

        assertThat(context.source()).isEqualTo(OperationalContextSource.NONE);
    }

    @Test
    void malformedIdsFailDuringParsing() {
        assertThatThrownBy(() -> parser.parseBridge(headers(
            OperationalContextHeaders.TERMINAL_ID, "not-a-uuid")::get))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, String> headers(String... values) {
        var builder = new java.util.HashMap<String, String>();

        for (int i = 0; i < values.length; i += 2) {
            builder.put(values[i], values[i + 1]);
        }

        return builder;
    }
}
