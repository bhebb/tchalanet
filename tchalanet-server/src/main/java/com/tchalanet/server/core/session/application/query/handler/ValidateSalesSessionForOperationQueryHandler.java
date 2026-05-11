package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.session.application.query.model.ValidatedSalesSessionOperationView;
import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidateSalesSessionForOperationQueryHandler
    implements QueryHandler<ValidateSalesSessionForOperationQuery, ValidatedSalesSessionOperationView> {

  private final SalesSessionReaderPort sessionReader;

  @Override
  public ValidatedSalesSessionOperationView handle(ValidateSalesSessionForOperationQuery q) {
    var session = sessionReader.getById(q.tenantId(), q.salesSessionId());

    if (!session.tenantId().equals(q.tenantId())) {
      throw ProblemRest.forbidden("sales_session.tenant_mismatch");
    }

    if (!session.terminalId().equals(q.terminalId())) {
      throw ProblemRest.forbidden("sales_session.terminal_mismatch");
    }

    if (!session.outletId().equals(q.outletId())) {
      throw ProblemRest.forbidden("sales_session.outlet_mismatch");
    }

    if (!session.sellerUserId().equals(q.sellerUserId())) {
      throw ProblemRest.forbidden("sales_session.seller_mismatch");
    }

    switch (q.operation()) {
      case SELL -> requireOpen(session.status(), "sales_session.not_open_for_sale");
      case PAYOUT -> requireOpen(session.status(), "sales_session.not_open_for_payout");
      case OFFLINE_GRANT -> requireOpen(session.status(), "sales_session.not_open_for_offline_grant");
      case CANCEL -> requireNotCancelledOrFinalized(session.status());
      case OFFLINE_SYNC -> requireNotCancelled(session.status());
    }

    return new ValidatedSalesSessionOperationView(
        session.id(),
        session.status(),
        session.openedAt(),
        session.closedAt(),
        session.finalizedAt(),
        session.finalizedBy());
  }

  private void requireOpen(SalesSessionStatus status, String code) {
    if (status != SalesSessionStatus.OPEN) {
      throw ProblemRest.conflict(code);
    }
  }

  private void requireNotCancelled(SalesSessionStatus status) {
    if (status == SalesSessionStatus.CANCELLED) {
      throw ProblemRest.conflict("sales_session.cancelled");
    }
  }

  private void requireNotCancelledOrFinalized(SalesSessionStatus status) {
    if (status == SalesSessionStatus.CANCELLED) {
      throw ProblemRest.conflict("sales_session.cancelled");
    }
    if (status == SalesSessionStatus.FINALIZED) {
      throw ProblemRest.conflict("sales_session.finalized");
    }
  }
}
