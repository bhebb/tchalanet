package com.tchalanet.server.features.runtime.model;

import java.util.List;

public record EntitlementsView(
    List<String> roles,
    List<String> permissions
) {}
