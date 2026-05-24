package com.tchalanet.server.core.promotion.internal.domain.service;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionCampaignTransition;
import org.springframework.stereotype.Component;

@Component
public final class PromotionCampaignStateMachine {

    public PromotionCampaignStatus apply(
        PromotionCampaignStatus current,
        PromotionCampaignTransition transition
    ) {
        if (current == null) {
            throw ProblemRest.conflict("promotion.campaign.status_missing");
        }

        return switch (transition) {
            case ACTIVATE -> activate(current);
            case PAUSE -> pause(current);
            case DEACTIVATE -> deactivate(current);
            case ARCHIVE -> archive(current);
        };
    }

    private PromotionCampaignStatus activate(PromotionCampaignStatus current) {
        return switch (current) {
            case DRAFT, INACTIVE, PAUSED -> PromotionCampaignStatus.ACTIVE;
            case ACTIVE -> throw ProblemRest.conflict("promotion.campaign.already_active");
            case ARCHIVED -> throw ProblemRest.conflict("promotion.campaign.archived_cannot_activate");
        };
    }

    private PromotionCampaignStatus pause(PromotionCampaignStatus current) {
        return switch (current) {
            case ACTIVE -> PromotionCampaignStatus.PAUSED;
            case PAUSED -> throw ProblemRest.conflict("promotion.campaign.already_paused");
            case DRAFT, INACTIVE -> throw ProblemRest.conflict("promotion.campaign.not_active");
            case ARCHIVED -> throw ProblemRest.conflict("promotion.campaign.archived_cannot_pause");
        };
    }

    private PromotionCampaignStatus deactivate(PromotionCampaignStatus current) {
        return switch (current) {
            case DRAFT, PAUSED -> PromotionCampaignStatus.INACTIVE;
            case INACTIVE -> throw ProblemRest.conflict("promotion.campaign.already_inactive");
            case ACTIVE -> throw ProblemRest.conflict("promotion.campaign.pause_before_deactivate");
            case ARCHIVED -> throw ProblemRest.conflict("promotion.campaign.archived_cannot_deactivate");
        };
    }

    private PromotionCampaignStatus archive(PromotionCampaignStatus current) {
        return switch (current) {
            case DRAFT, INACTIVE, PAUSED -> PromotionCampaignStatus.ARCHIVED;
            case ACTIVE -> throw ProblemRest.conflict("promotion.campaign.pause_before_archive");
            case ARCHIVED -> throw ProblemRest.conflict("promotion.campaign.already_archived");
        };
    }
}
