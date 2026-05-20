package com.tchalanet.server.common.context.operational;

public sealed interface OperationalRequestContext
    permits PosOperationalContext, SuperAdminOperationalContext {

    OperationalContextRole role();

    OperationalContextSource source();

    OperationalContextTrust trustLevel();

    default boolean trustedForSensitiveOperation() {
        return trustLevel() == OperationalContextTrust.STRONG;
    }
}
