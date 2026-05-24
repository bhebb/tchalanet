package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AppliedPromotionId(UUID value) {
    public AppliedPromotionId {
        if (value == null) throw new IllegalArgumentException("AppliedPromotionId.value is null");
    }
    public static AppliedPromotionId of(UUID value) { return new AppliedPromotionId(value); }
    public static AppliedPromotionId nullableOf(UUID value) { return value == null ? null : new AppliedPromotionId(value); }
    public static AppliedPromotionId parse(String raw) { return new AppliedPromotionId(UUID.fromString(raw)); }
}

