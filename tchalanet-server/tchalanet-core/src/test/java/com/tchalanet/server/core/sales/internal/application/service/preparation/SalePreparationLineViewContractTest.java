package com.tchalanet.server.core.sales.internal.application.service.preparation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationLineView;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SalePreparationLineViewContractTest {

    @Test
    void previewLineExposesCommercialOptionWithoutTechnicalPricingVariant() {
        var componentNames = Arrays.stream(SalePreparationLineView.class.getRecordComponents())
            .map(component -> component.getName())
            .toList();

        assertThat(componentNames)
            .contains("betType", "betOption", "oddsSnapshot")
            .doesNotContain("pricingVariantCode", "settlementVariant");
    }
}
