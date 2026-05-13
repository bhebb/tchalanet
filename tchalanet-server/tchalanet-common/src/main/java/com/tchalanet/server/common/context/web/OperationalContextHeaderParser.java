package com.tchalanet.server.common.context.web;

import com.tchalanet.server.common.context.OperationalContextSource;
import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHeaders;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class OperationalContextHeaderParser {

    @FunctionalInterface
    public interface HeaderReader {
        String getHeader(String name);
    }

    public OperationalRequestContext parseBridge(HttpServletRequest request) {
        return parseBridge(request::getHeader);
    }

    public OperationalRequestContext parseBridge(HeaderReader headers) {
        var hint = parseHint(headers);

        if (!hint.hasPosFrame()) {
            return OperationalRequestContext.empty();
        }

        return new OperationalRequestContext(
            hint.terminalId(),
            hint.outletId(),
            hint.salesSessionId(),
            toBridgeSource(hint.source()));
    }

    public OperationalContextHint parseHint(HttpServletRequest request) {
        return parseHint(request::getHeader);
    }

    public OperationalContextHint parseHint(HeaderReader headers) {
        var terminalId = parseTerminalId(header(headers, OperationalContextHeaders.TERMINAL_ID));
        var outletId = parseOutletId(header(headers, OperationalContextHeaders.OUTLET_ID));
        var salesSessionId =
            parseSalesSessionId(header(headers, OperationalContextHeaders.SALES_SESSION_ID));
        var tenantOverride =
            parseTenantId(header(headers, OperationalContextHeaders.SUPER_ADMIN_TENANT_OVERRIDE));
        var overrideReason = header(headers, OperationalContextHeaders.SUPER_ADMIN_OVERRIDE_REASON);
        var source = resolveSource(header(headers, OperationalContextHeaders.OPERATIONAL_SOURCE));

        if (source == com.tchalanet.server.common.context.operational.OperationalContextSource.NONE
            && (terminalId != null || outletId != null || salesSessionId != null)) {
            source = com.tchalanet.server.common.context.operational.OperationalContextSource.CLIENT_CLAIM;
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

        if (StringUtils.isNotBlank(value)) {
            return value.trim();
        }

        return null;
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

    private static com.tchalanet.server.common.context.operational.OperationalContextSource resolveSource(
        String value) {

        if (StringUtils.isBlank(value)) {
            return com.tchalanet.server.common.context.operational.OperationalContextSource.NONE;
        }

        return com.tchalanet.server.common.context.operational.OperationalContextSource.valueOf(
            value.trim().toUpperCase(Locale.ROOT));
    }

    private static OperationalContextSource toBridgeSource(
        com.tchalanet.server.common.context.operational.OperationalContextSource source) {

        return OperationalContextSource.valueOf(source.name());
    }
}
