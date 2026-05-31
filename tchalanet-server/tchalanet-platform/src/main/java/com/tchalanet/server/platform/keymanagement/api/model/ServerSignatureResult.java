package com.tchalanet.server.platform.keymanagement.api.model;

public record ServerSignatureResult(
    String signature,
    String algorithm,
    String keyId
) {}
