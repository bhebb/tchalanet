package com.tchalanet.server.features.bootstrap;

import java.util.List;

public record EntitlementsView(
    List<String> roles,
    List<String> permissions
) {}
