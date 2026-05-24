package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionQuotaId(UUID value) {
    public PromotionQuotaId {
        if (value == null) throw new IllegalArgumentException("PromotionQuotaId.value is null");
    }
    public static PromotionQuotaId of(UUID value) { return new PromotionQuotaId(value); }
    public static PromotionQuotaId nullableOf(UUID value) { return value == null ? null : new PromotionQuotaId(value); }
    public static PromotionQuotaId parse(String raw) { return new PromotionQuotaId(UUID.fromString(raw)); }
}

