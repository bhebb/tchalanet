package com.tchalanet.server.core.terminal.internal.application.service.binding;

import com.tchalanet.server.common.crypto.Hashing;
import java.util.Objects;

public final class TerminalPublicKeyHasher {

    private TerminalPublicKeyHasher() {}

    public static String hash(String publicKeyBase64) {
        Objects.requireNonNull(publicKeyBase64, "publicKeyBase64 is required");
        if (publicKeyBase64.isBlank()) {
            throw new IllegalArgumentException("publicKeyBase64 must not be blank");
        }
        return Hashing.sha256Hex(publicKeyBase64.trim());
    }
}
