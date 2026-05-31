package com.tchalanet.server.platform.keymanagement.api.model;

import java.time.Instant;

public record BackendPublicKeyView(
    String keyId,
    String algorithm,
    String publicKeyFormat,
    String publicKey,
    Instant validFrom,
    Instant validUntil,
    String status
) {}
