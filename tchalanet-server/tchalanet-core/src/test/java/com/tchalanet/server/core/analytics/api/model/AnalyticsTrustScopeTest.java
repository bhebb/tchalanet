package com.tchalanet.server.core.analytics.api.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsTrustScopeTest {

  @Test
  void drawScopeRejectsMultipleBusinessDates() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var refDate = LocalDate.of(2026, 7, 17);

    assertThatThrownBy(
            () ->
                new AnalyticsTrustScope(
                    AnalyticsTrustScopeType.DRAW,
                    tenantId,
                    null,
                    drawId,
                    refDate,
                    refDate.plusDays(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("draw trust scope must target one business date");
  }
}
