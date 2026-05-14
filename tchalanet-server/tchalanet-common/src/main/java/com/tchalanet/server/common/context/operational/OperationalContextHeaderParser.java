package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
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
        var tenantOverride =
            parseTenantId(header(headers, OperationalContextHeaders.SUPER_ADMIN_TENANT_OVERRIDE));
        var overrideReason = header(headers, OperationalContextHeaders.SUPER_ADMIN_OVERRIDE_REASON);
        var source = resolveSource(header(headers, OperationalContextHeaders.OPERATIONAL_SOURCE));

        if (source == OperationalContextSource.NONE
            && (terminalId != null || outletId != null || salesSessionId != null)) {
            source = OperationalContextSource.CLIENT_CLAIM;
        }

        return new OperationalContextHint(
            terminalId,
            outletId,
            salesSessionId,
            tenantOverride,
            overrideReason,
            source);
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

    private static TenantId parseTenantId(String value) {
        return value == null ? null : TenantId.parse(value);
    }

    private static OperationalContextSource resolveSource(String value) {
        if (StringUtils.isBlank(value)) {
            return OperationalContextSource.NONE;
        }

        return OperationalContextSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
