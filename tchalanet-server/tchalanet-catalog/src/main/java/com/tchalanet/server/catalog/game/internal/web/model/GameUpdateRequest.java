package com.tchalanet.server.catalog.game.internal.web.model;

public record GameUpdateRequest(
    String name,
    String category,
    String combination,
    Integer minDigits,
    Integer maxDigits,
    String description,
    Boolean active,
    Integer sortOrder
) {}
