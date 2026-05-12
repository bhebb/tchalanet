package com.tchalanet.server.common.context;

public enum OperationalContextSource {
    NONE(TrustLevel.NONE),
    CLIENT_CLAIM(TrustLevel.WEAK),
    SERVER_BOOTSTRAP(TrustLevel.STRONG),
    SIGNED_DEVICE_BINDING(TrustLevel.STRONG),
    ADMIN_SELECTION(TrustLevel.STRONG);

    private final TrustLevel trustLevel;

    OperationalContextSource(TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public TrustLevel trustLevel() {
        return trustLevel;
    }

    public boolean isTrustedForSensitiveOperation() {
        return trustLevel == TrustLevel.STRONG;
    }
}
