package com.tchalanet.server.common.context.operational;

public record OperationalContextHint(
    OperationalContextSource source,
    OperationalContextTrust trust
) {

    public boolean trustedForSensitiveOperation() {
        return trust == OperationalContextTrust.STRONG;
    }
}
