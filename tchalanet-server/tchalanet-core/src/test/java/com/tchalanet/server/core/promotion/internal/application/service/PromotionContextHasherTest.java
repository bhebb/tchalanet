package com.tchalanet.server.core.promotion.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionContextHasherTest {

  private final PromotionContextHasher hasher = new PromotionContextHasher();

  @Test
  void includesSellerTerminalIdInHash() {
    var first =
        context(SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001")));
    var second =
        context(SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000002")));

    assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
  }

  private static PromotionEvaluationContext context(SellerTerminalId sellerTerminalId) {
    return new PromotionEvaluationContext(
        TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        PromotionEvaluationPhase.SALE_CONFIRMATION,
        Instant.parse("2026-05-27T00:00:00Z"),
        null,
        List.of(),
        null,
        List.of(),
        sellerTerminalId,
        null,
        new BigDecimal("500.00"),
        "HTG",
        List.of("HT_BOLET"),
        false);
  }
}
