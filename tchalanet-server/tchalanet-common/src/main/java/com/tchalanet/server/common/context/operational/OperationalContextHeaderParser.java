package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;


public final class OperationalContextHeaderParser {

    @FunctionalInterface
    public interface HeaderReader {
        String getHeader(String name);
    }

    public static OperationalContextHint parseHint(HeaderReader headers) {
        var terminalId = parseTerminalId(header(headers, OperationalContextHeaders.TERMINAL_ID));
        var outletId = parseOutletId(header(headers, OperationalContextHeaders.OUTLET_ID));
        var salesSessionId =
            parseSalesSessionId(header(headers, OperationalContextHeaders.SALES_SESSION_ID));
        var declaredSource = resolveSource(header(headers, OperationalContextHeaders.OPERATIONAL_SOURCE));
        var source = deriveSource(declaredSource, terminalId, outletId, salesSessionId);
        var trust = deriveTrust(source);

        return new OperationalContextHint(terminalId, outletId, salesSessionId, source, trust);
    }

    private static String header(HeaderReader headers, String name) {
        var value = headers.getHeader(name);
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }

    private static TerminalId parseTerminalId(String value) {
        return value == null ? null : TerminalId.parse(value);
    }

    private static OutletId parseOutletId(String value) {
        return value == null ? null : OutletId.parse(value);
    }

    private static SalesSessionId parseSalesSessionId(String value) {
        return value == null ? null : SalesSessionId.parse(value);
    }

    private static OperationalContextSource resolveSource(String value) {
        if (StringUtils.isBlank(value)) {
            return OperationalContextSource.NONE;
        }

        return OperationalContextSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static OperationalContextSource deriveSource(
        OperationalContextSource declaredSource,
        TerminalId terminalId,
        OutletId outletId,
        SalesSessionId salesSessionId) {

        if (terminalId == null && outletId == null && salesSessionId == null) {
            return OperationalContextSource.NONE;
        }

        return OperationalContextSource.CLIENT_CLAIM;
    }

    private static OperationalContextTrust deriveTrust(OperationalContextSource source) {
        return switch (source) {
            case NONE -> OperationalContextTrust.NONE;
            case CLIENT_CLAIM -> OperationalContextTrust.WEAK;
            case SIGNED_DEVICE_BINDING, SERVER_BOOTSTRAP -> OperationalContextTrust.STRONG;
            case ADMIN_SELECTION -> OperationalContextTrust.STRONG;
        };
    }
}
