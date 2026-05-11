package com.tchalanet.server.core.sales.application.service;

import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;
import com.tchalanet.server.core.sales.application.command.model.OfflineTicketSaleInput;
import com.tchalanet.server.core.sales.application.port.out.DrawLookupPort.DrawSnapshot;
import com.tchalanet.server.core.sales.application.port.out.LimitPolicyPort.LimitDecision;
import com.tchalanet.server.core.sales.application.port.out.SalesSessionLookupPort.SalesSessionSnapshot;
import com.tchalanet.server.core.sales.domain.model.SalesSessionPostingMode;
import org.springframework.stereotype.Component;

@Component
public class OfflineSalesGateEvaluator {

  public GateDecision evaluate(
      OfflineTicketSaleInput input,
      DrawSnapshot draw,
      SalesSessionSnapshot session,
      LimitDecision limitDecision) {

    if (draw.resultedAt() != null) {
      return GateDecision.reject(SalesOfflineRejectReason.SYNC_AFTER_RESULT_KNOWN);
    }
    if (input.createdAtDevice().isAfter(draw.cutoffAt())) {
      return GateDecision.reject(SalesOfflineRejectReason.DRAW_CUTOFF_PASSED);
    }
    if (limitDecision.blocked()) {
      return GateDecision.reject(SalesOfflineRejectReason.LIMIT_POLICY_BLOCKED);
    }
    if (session.finalized()) {
      return GateDecision.review(SalesOfflineRejectReason.SESSION_FINALIZED);
    }
    if (session.closedAt() != null && input.createdAtDevice().isAfter(session.closedAt())) {
      return GateDecision.reject(SalesOfflineRejectReason.SESSION_CLOSED_BEFORE_SALE);
    }
    if (session.closedAt() != null) {
      return new GateDecision(SalesOfflineDecision.ACCEPTED_POST_CLOSE_ADJUSTMENT, null, SalesSessionPostingMode.POST_CLOSE_ADJUSTMENT);
    }
    return new GateDecision(SalesOfflineDecision.ACCEPTED, null, SalesSessionPostingMode.NORMAL_OPEN_SESSION);
  }

  public record GateDecision(
      SalesOfflineDecision decision,
      SalesOfflineRejectReason rejectReason,
      SalesSessionPostingMode postingMode
  ) {
    public static GateDecision reject(SalesOfflineRejectReason reason) {
      return new GateDecision(SalesOfflineDecision.REJECTED, reason, null);
    }
    public static GateDecision review(SalesOfflineRejectReason reason) {
      return new GateDecision(SalesOfflineDecision.REVIEW_REQUIRED, reason, null);
    }
    public boolean accepted() {
      return decision == SalesOfflineDecision.ACCEPTED || decision == SalesOfflineDecision.ACCEPTED_POST_CLOSE_ADJUSTMENT;
    }
  }
}
