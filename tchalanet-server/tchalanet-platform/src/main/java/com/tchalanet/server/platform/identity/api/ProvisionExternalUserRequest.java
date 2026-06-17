package com.tchalanet.server.platform.identity.api;

public record ProvisionExternalUserRequest(
    String requestedExternalSubject,
    String email,
    String phone,
    String displayName,
    String initialPassword) {}

