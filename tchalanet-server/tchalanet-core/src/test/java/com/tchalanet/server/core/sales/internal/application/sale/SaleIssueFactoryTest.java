package com.tchalanet.server.core.sales.internal.application.sale;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.promotion.api.model.PromotionNoticeCodes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaleIssueFactoryTest {

  private final SaleIssueFactory factory = new SaleIssueFactory();

  @Test
  void mapsPromotionNoticesToStableSaleIssueCodes() {
    var issues =
        factory.fromNotices(
            List.of(
                new ApiNotice(
                    PromotionNoticeCodes.DECISION_APPLIED,
                    PromotionNoticeCodes.DECISION_APPLIED,
                    "promotion",
                    NoticeSeverity.INFO,
                    Map.of()),
                new ApiNotice(
                    PromotionNoticeCodes.TERMINAL_OVERRIDE_APPLIED,
                    PromotionNoticeCodes.TERMINAL_OVERRIDE_APPLIED,
                    "promotion",
                    NoticeSeverity.INFO,
                    Map.of())));

    assertThat(issues)
        .extracting(issue -> issue.code())
        .containsExactly("sales.promotion_applied", "sales.promotion_terminal_override_applied");
  }
}
