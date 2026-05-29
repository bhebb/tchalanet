package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignReadPort;
import com.tchalanet.server.core.promotion.internal.infra.cache.PromotionCacheSpecProvider;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PromotionCampaignReadJpaAdapter implements PromotionCampaignReadPort {

    private final PromotionCampaignProjectionRepository repository;
    private final PromotionCampaignViewAssembler promotionCampaignViewAssembler;
    private final TchContextResolver tchContextResolver;

    @Override
    @Cacheable(
        value = PromotionCacheSpecProvider.PROMOTION_CAMPAIGN_ADMIN_LIST,
        key = "#root.target.adminListKey(#pageable)"
    )
    public TchPage<PromotionCampaignView> findCampaigns(Pageable pageable) {
        var page = repository.findSummaries(pageable);

        return TchPageMapper.map(page, p -> new PromotionCampaignView(
            PromotionCampaignId.of(p.id()),
            p.code(),
            p.name(),
            p.status(),
            p.priority(),
            p.startsAt(),
            p.endsAt(),
            List.of()
        ));
    }

    @Override
    @Cacheable(
        value = PromotionCacheSpecProvider.PROMOTION_CAMPAIGN_BY_ID,
        key = "#root.target.campaignCacheKey(#id)"
    )
    public Optional<PromotionCampaignView> findById(PromotionCampaignId id) {
        if (!repository.existsById(id.value())) {
            return Optional.empty();
        }

        return Optional.of(promotionCampaignViewAssembler.toCampaignView(id.value()));
    }

    @Override
    @Cacheable(
        value = PromotionCacheSpecProvider.PROMOTION_CAMPAIGN_BY_ID,
        key = "#root.target.campaignCacheKey(#promotionCampaignId)"
    )
    public PromotionCampaignView getRequired(PromotionCampaignId promotionCampaignId) {
        return findById(promotionCampaignId)
            .orElseThrow(() -> ProblemRest.notFound("promotion.campaign.not_found"));
    }

    public String tenantKey() {
        var ctx = tchContextResolver.currentOrNull();

        if (ctx == null || ctx.tenantIdSafe() == null) {
            return "__no_tenant";
        }

        return ctx.tenantIdSafe().value().toString();
    }

    public String campaignCacheKey(PromotionCampaignId id) {
        return tenantKey() + ":" + id.value();
    }

    public String adminListKey(Pageable pageable) {
        return tenantKey()
            + ":page=" + pageable.getPageNumber()
            + ":size=" + pageable.getPageSize()
            + ":sort=" + pageable.getSort();
    }
}
