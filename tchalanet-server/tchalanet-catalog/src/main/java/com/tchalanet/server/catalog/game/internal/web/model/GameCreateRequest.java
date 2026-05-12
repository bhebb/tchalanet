package com.tchalanet.server.catalog.game.internal.web.model;

public record GameCreateRequest(
    String code,
    String name,
    String category,
    String combination,
    Integer minDigits,
    Integer maxDigits,
    String description,
    Boolean active,
    Integer sortOrder
) {}
