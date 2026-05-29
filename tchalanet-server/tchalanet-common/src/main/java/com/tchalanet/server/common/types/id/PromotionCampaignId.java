package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionCampaignId(UUID value) {
    public PromotionCampaignId {
        if (value == null) throw new IllegalArgumentException("PromotionCampaignId.value is null");
    }
    public static PromotionCampaignId of(UUID value) { return new PromotionCampaignId(value); }
    public static PromotionCampaignId nullableOf(UUID value) { return value == null ? null : new PromotionCampaignId(value); }
    public static PromotionCampaignId parse(String raw) { return new PromotionCampaignId(UUID.fromString(raw)); }
}

