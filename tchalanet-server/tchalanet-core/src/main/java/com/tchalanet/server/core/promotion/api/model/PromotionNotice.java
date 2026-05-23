package com.tchalanet.server.core.promotion.api.model;

import java.util.Map;

public record PromotionNotice(
    String code,
    String message,
    String severity,
    Map<String, Object> meta
) {}
