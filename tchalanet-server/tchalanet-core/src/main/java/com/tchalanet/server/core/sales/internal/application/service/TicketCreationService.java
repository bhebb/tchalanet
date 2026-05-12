package com.tchalanet.server.core.sales.internal.application.service;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.domain.model.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketCreationService {

  private final Clock clock;

  public Ticket createAccepted(
      TicketId ticketId,
      TenantId tenantId,
      OutletId outletId,
      TerminalId terminalId,
      UserId sellerUserId,
      SalesSessionId sessionId,
      DrawId drawId,
      DrawChannelId drawChannelId,
      String ticketCode,
      String publicCode,
      String verificationCode,
      CurrencyCode currency,
      TicketMoneyBreakdown money,
      BigDecimal potentialPayout,
      SaleOrigin saleOrigin,
      TicketSyncStatus syncStatus,
      OfflineSaleRef offlineSaleRef,
      SalesSessionPostingMode postingMode,
      List<TicketLine> lines) {

    return new Ticket(
        ticketId,
        tenantId,
        outletId,
        terminalId,
        sellerUserId,
        sessionId,
        drawId,
        drawChannelId,
        ticketCode,
        publicCode,
        verificationCode,
        currency,
        money,
        potentialPayout,
        null,
        TicketSaleStatus.SOLD,
        TicketResultStatus.NOT_RESULTED,
        TicketSettlementStatus.UNSETTLED,
        saleOrigin,
        syncStatus,
        offlineSaleRef,
        postingMode,
        clock.instant(),
        null,
        null,
        null,
        null,
        lines);
  }
}
