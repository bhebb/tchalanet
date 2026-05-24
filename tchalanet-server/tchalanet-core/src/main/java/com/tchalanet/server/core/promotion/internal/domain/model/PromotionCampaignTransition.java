package com.tchalanet.server.core.promotion.internal.domain.model;

import com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus;

public enum PromotionCampaignTransition {
    ACTIVATE,
    PAUSE,
    DEACTIVATE,
    ARCHIVE;

    public PromotionCampaignStatus targetStatus() {
        return switch (this) {
            case ACTIVATE -> PromotionCampaignStatus.ACTIVE;
            case PAUSE -> PromotionCampaignStatus.PAUSED;
            case DEACTIVATE -> PromotionCampaignStatus.INACTIVE;
            case ARCHIVE -> PromotionCampaignStatus.ARCHIVED;
        };
    }
}
