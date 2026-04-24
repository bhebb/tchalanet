package com.tchalanet.server.catalog.pricing.internal.mapper;

import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import org.springframework.stereotype.Component;

@Component
public class PricingEntityMapper {

  public PricingOddsView toView(PricingOddsEntity e) {
    if (e == null) return null;
    return new PricingOddsView(
        e.getId(),
        e.getTenantId(),
        e.getGameCode(),
        e.getBetType(),
        e.getBetOption(),
        e.getOdds(),
        e.isActive()
    );
  }
}
