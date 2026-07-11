package com.tchalanet.server.core.sales.api.model.settlement;

import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;

public enum SettlementRuleCode {
    MATCH_1_2D,
    MATCH_2_2D,
    MATCH_3_2D,
    MARRIAGE_EXACT_ORDER,
    MARRIAGE_REVERSE_ALLOWED,
    LOTTO3_STRAIGHT,
    LOTTO3_BOX_3_WAY,
    LOTTO3_BOX_6_WAY,
    LOTTO4_STRAIGHT,
    LOTTO4_BOX_4_WAY,
    LOTTO4_BOX_6_WAY,
    LOTTO4_BOX_12_WAY,
    LOTTO4_BOX_24_WAY,
    LOTTO4_FRONT_PAIR,
    LOTTO4_BACK_PAIR,
    LOTTO5_LOT1_LOT2,
    LOTTO5_LOT1_LOT3,
    LOTTO5_MIXED_1_2_3;

    public static SettlementRuleCode fromPricingVariant(PricingVariantCode variantCode) {
        return SettlementRuleCode.valueOf(variantCode.name());
    }

    public PricingVariantCode toPricingVariantCode() {
        return PricingVariantCode.valueOf(name());
    }
}
