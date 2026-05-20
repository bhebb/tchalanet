package com.tchalanet.server.core.offlinesync.internal.domain.model.grant;

import java.util.UUID;

/** Cryptographic material bound to the POS device this grant was issued for. */
public record GrantDevice(
    UUID deviceId,
    String devicePublicKey,
    String keyId
) {
    public GrantDevice {
        if (deviceId == null) throw new IllegalArgumentException("deviceId required");
        if (devicePublicKey == null || devicePublicKey.isBlank())
            throw new IllegalArgumentException("devicePublicKey required");
        if (keyId == null || keyId.isBlank())
            throw new IllegalArgumentException("keyId required");
    }
}
