package com.tchalanet.server.common.context.operational;

public sealed interface OperationalRequestContext
    permits PosOperationalContext, SuperAdminOperationalContext {

    OperationalContextRole role();

    OperationalContextSource source();

    TrustLevel trustLevel();

    default boolean trustedForSensitiveOperation() {
        return trustLevel() == TrustLevel.STRONG;
    }
}
