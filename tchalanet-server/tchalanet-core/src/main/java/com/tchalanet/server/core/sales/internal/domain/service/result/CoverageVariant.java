package com.tchalanet.server.core.sales.internal.domain.service.result;

import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import java.util.Objects;

public record CoverageVariant(
    PricingVariantCode pricingVariantCode,
    WinMode winMode
) {

    public CoverageVariant {
        Objects.requireNonNull(pricingVariantCode, "coverage_variant.pricing_variant_required");
        winMode = winMode == null ? WinMode.ALTERNATIVE : winMode;
    }
}
