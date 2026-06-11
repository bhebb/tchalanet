package com.tchalanet.server.features.runtime.model;

import java.util.List;

public record AuthenticatedUserView(
    String userId,
    String username,
    String displayName,
    String email,
    List<String> roles,
    PrivateBootstrapSpace defaultSpace,
    String preferredLocale,
    String preferredTimezone
) {}
