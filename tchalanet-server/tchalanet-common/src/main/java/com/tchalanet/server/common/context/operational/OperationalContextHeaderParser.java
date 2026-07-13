package com.tchalanet.server.common.context.operational;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class OperationalContextHeaderParser {

    private static final Logger log = LoggerFactory.getLogger(OperationalContextHeaderParser.class);

    @FunctionalInterface
    public interface HeaderReader {
        String getHeader(String name);
    }

    public static OperationalContextHint parseHint(HeaderReader headers) {
          var declaredSource = resolveSource(header(headers, OperationalContextHeaders.OPERATIONAL_SOURCE));
        var source = deriveSource(declaredSource);
        var trust = deriveTrust(source);

        return new OperationalContextHint(source, trust);
    }

    private static String header(HeaderReader headers, String name) {
        var value = headers.getHeader(name);
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }



    private static OperationalContextSource resolveSource(String value) {
        if (StringUtils.isBlank(value)) {
            return OperationalContextSource.NONE;
        }

        return OperationalContextSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static OperationalContextSource deriveSource(
        OperationalContextSource declaredSource) {

        if (declaredSource == OperationalContextSource.ADMIN_SELECTION) {
            log.warn("tch.context.admin-selection-without-token");
        }

        if (declaredSource == OperationalContextSource.SIGNED_DEVICE_BINDING) {
            log.warn("tch.context.signed-binding-without-server-verification");
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
