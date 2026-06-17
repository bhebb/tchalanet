package com.tchalanet.server.platform.identity.api.model.request;

import java.time.ZoneId;
import java.util.Locale;

public record BootstrapCurrentUserRequest(
    String externalSubject,
    String tenantCode,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    Locale locale,
    ZoneId timeZone) {}
