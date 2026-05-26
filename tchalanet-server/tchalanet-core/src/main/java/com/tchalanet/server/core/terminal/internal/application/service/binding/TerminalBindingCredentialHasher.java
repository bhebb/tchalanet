package com.tchalanet.server.core.terminal.internal.application.service.binding;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TerminalBindingCredentialHasher {

    private TerminalBindingCredentialHasher() {}

    public static String hash(TenantId tenantId, TerminalId terminalId, String credential) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("credential is required");
        }

        return Hashing.sha256Hex(
            tenantId.value() + "|" + terminalId.value() + "|" + credential.trim()
        );
    }

    public static boolean matches(
        TenantId tenantId,
        TerminalId terminalId,
        String credential,
        String expectedHash
    ) {
        if (credential == null || credential.isBlank() || expectedHash == null || expectedHash.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
            hash(tenantId, terminalId, credential).getBytes(StandardCharsets.UTF_8),
            expectedHash.trim().getBytes(StandardCharsets.UTF_8)
        );
    }
}
