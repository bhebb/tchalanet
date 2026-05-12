package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.core.sales.application.query.model.PublicTicketVerificationRecord;
import com.tchalanet.server.features.ticketverify.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketVerifyMapper {

  public TicketVerifyResponse toExpiredResponse(String publicCode) {
    return new TicketVerifyResponse(
        TicketVerifyStatus.EXPIRED, publicCode, TicketVerifyPayoutStatus.EXPIRED,
        null, null, null, null, List.of());
  }

  public TicketVerifyResponse toVoidResponse(String publicCode) {
    return new TicketVerifyResponse(
        TicketVerifyStatus.VOID, publicCode, TicketVerifyPayoutStatus.VOID,
        null, null, null, null, List.of());
  }

  public TicketVerifyResponse toResponse(PublicTicketVerificationRecord r, boolean expired) {
    if (expired) return toExpiredResponse(r.publicCode());

    if (isVoided(r.saleStatus())) return toVoidResponse(r.publicCode());

    var payoutStatus = resolvePayoutStatus(r);
    var outlet = (r.outletName() != null)
        ? new TicketVerifyOutletView(r.outletName(), r.outletCity(), r.outletCountry())
        : null;

    var lines = r.lines().stream()
        .map(l -> new TicketVerifyLineItem(
            l.gameCode(),
            l.betType() != null ? l.betType().name() : null,
            l.selection(),
            l.stake(),
            l.potentialPayout()))
        .toList();

    return new TicketVerifyResponse(
        TicketVerifyStatus.VALID,
        r.publicCode(),
        payoutStatus,
        r.totalAmount(),
        r.winningAmount(),
        r.createdAt(),
        outlet,
        lines
    );
  }

  private TicketVerifyPayoutStatus resolvePayoutStatus(PublicTicketVerificationRecord r) {
    if (isVoided(r.saleStatus())) return TicketVerifyPayoutStatus.VOID;
    if (r.resultStatus() == TicketResultStatus.NOT_RESULTED) return TicketVerifyPayoutStatus.PENDING_DRAW;
    if (r.resultStatus() == TicketResultStatus.LOST) return TicketVerifyPayoutStatus.LOST;
    if (r.resultStatus() == TicketResultStatus.WON || r.resultStatus() == TicketResultStatus.OVERRIDDEN) {
      return r.settlementStatus() == TicketSettlementStatus.SETTLED
          ? TicketVerifyPayoutStatus.WON_PAID
          : TicketVerifyPayoutStatus.WON_UNCLAIMED;
    }
    return TicketVerifyPayoutStatus.UNKNOWN;
  }

  private boolean isVoided(TicketSaleStatus status) {
    return status == TicketSaleStatus.VOID || status == TicketSaleStatus.REJECTED;
  }
}
