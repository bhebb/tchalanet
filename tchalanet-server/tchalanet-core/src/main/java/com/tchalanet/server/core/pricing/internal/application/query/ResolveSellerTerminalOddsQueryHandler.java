package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolveSellerTerminalOddsQueryHandler
    implements QueryHandler<ResolveSellerTerminalOddsQuery, SellerTerminalOddsResolutionView> {

  private final SellerTerminalOddsOverrideReaderPort reader;
  private final TenantPricingOddsReaderPort tenantOddsReader;

  @Override
  public SellerTerminalOddsResolutionView handle(ResolveSellerTerminalOddsQuery q) {
    BigDecimal tenantDefault =
        tenantOddsReader
            .findByNaturalKey(
                q.tenantId(),
                TenantPricingOdds.normalizeGameCode(q.gameCode()),
                q.pricingVariantCode())
            .map(TenantPricingOdds::odds)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "tenant default pricing odds missing for tenant=%s game=%s variant=%s"
                            .formatted(q.tenantId(), q.gameCode(), q.pricingVariantCode())));

    // Look for active seller_terminal override
    var override =
        reader.findActiveByNaturalKey(
            q.tenantId(), q.sellerTerminalId(),
            TenantPricingOdds.normalizeGameCode(q.gameCode()), q.pricingVariantCode());

    if (override.isPresent()) {
      var o = override.get();
      return new SellerTerminalOddsResolutionView(
          q.gameCode(),
          q.pricingVariantCode(),
          q.betType(),
          q.betOption(),
          tenantDefault,
          o.odds(),
          o.odds(),
          OddsSource.SELLER_TERMINAL_OVERRIDE);
    }

    return new SellerTerminalOddsResolutionView(
        q.gameCode(),
        q.pricingVariantCode(),
        q.betType(),
        q.betOption(),
        tenantDefault,
        null,
        tenantDefault,
        OddsSource.TENANT_DEFAULT);
  }
}
