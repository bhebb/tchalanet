package com.tchalanet.server.common.context.operational;

public enum OperationalContextSource {
    NONE(OperationalContextTrust.NONE),
    CLIENT_CLAIM(OperationalContextTrust.WEAK),
    SERVER_BOOTSTRAP(OperationalContextTrust.STRONG),
    SIGNED_DEVICE_BINDING(OperationalContextTrust.STRONG),
    ADMIN_SELECTION(OperationalContextTrust.STRONG),
    SUPER_ADMIN_OVERRIDE(OperationalContextTrust.STRONG);

    private final OperationalContextTrust trustLevel;

    OperationalContextSource(OperationalContextTrust trustLevel) {
        this.trustLevel = trustLevel;
    }

    public OperationalContextTrust trustLevel() {
        return trustLevel;
    }

    public boolean isTrustedForSensitiveOperation() {
        return trustLevel == OperationalContextTrust.STRONG;
    }
}
