package com.tchalanet.server.core.sales.internal.application.sale;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.model.PromotionNoticeCodes;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.sales.internal.application.service.sell.SalesNoticeAssembler;
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
                    null,
                    "promotion",
                    NoticeSeverity.INFO,
                    Map.of()),
                new ApiNotice(
                    PromotionNoticeCodes.TERMINAL_OVERRIDE_APPLIED,
                    null,
                    "promotion",
                    NoticeSeverity.INFO,
                    Map.of())));

    assertThat(issues)
        .extracting(issue -> issue.code())
        .containsExactly("sales.promotion_applied", "sales.promotion_terminal_override_applied");
  }

  @Test
  void preserves_code_first_problem_detail_code_without_parsing_detail() {
    var issue = factory.fromProblem(ProblemRest.of(SalesErrorCodes.PREPARATION_EXPIRED));

    assertThat(issue.code()).isEqualTo("sales.preparation.expired");
    assertThat(issue.message()).isEqualTo("sales.preparation.expired.message");
  }

  @Test
  void preserves_seller_terminal_sale_gate_code() {
    var issue = factory.fromProblem(ProblemRest.of(SalesErrorCodes.SELLER_TERMINAL_CANNOT_SELL));

    assertThat(issue.code()).isEqualTo("sales.seller_terminal.cannot_sell");
  }

  @Test
  void approvalNoticeUsesTheStableSalesCodeRatherThanATransportSentinel() {
    var notice = SalesNoticeAssembler.approvalRequired("SUPERVISOR");

    assertThat(notice.code()).isEqualTo("sales.approval_required");
    assertThat(notice.message()).isNull();
    assertThat(notice.params()).containsEntry("approvalLevel", "SUPERVISOR");
  }
}
