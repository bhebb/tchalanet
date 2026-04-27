package com.tchalanet.server.catalog.pricing.internal.write;

import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import com.tchalanet.server.common.types.id.PricingOddsId;
import com.tchalanet.server.common.types.id.TenantId;
import org.springframework.stereotype.Component;

@Component
public class PricingEntityMapper {

  public PricingOddsView toView(PricingOddsEntity e) {
    if (e == null) return null;
    return new PricingOddsView(
        PricingOddsId.nullableOf(e.getId()),
        TenantId.nullableOf(e.getTenantId()),
        e.getGameCode(),
        e.getBetType(),
        e.getBetOption(),
        e.getOdds(),
        e.isActive()
    );
  }
}
