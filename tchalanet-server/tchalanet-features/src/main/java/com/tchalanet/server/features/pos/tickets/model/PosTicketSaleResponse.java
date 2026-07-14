package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.util.List;

public record PosTicketSaleResponse(
    SellTicketOutcome outcome,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    TicketSaleStatus saleStatus,
    ApprovalRequestId approvalRequestId,
    List<SaleIssueView> issues,
    TicketBackupInfo backup,
    SaleActionAvailability actionAvailability,
    String sellerInstruction) {

  public static PosTicketSaleResponse from(SellTicketResult result) {
    return new PosTicketSaleResponse(
        result.outcome(),
        result.ticketId(),
        result.ticketCode(),
        result.publicCode(),
        result.displayCode(),
        result.saleStatus(),
        result.approvalRequestId(),
        result.issues(),
        result.backup(),
        result.actionAvailability(),
        result.sellerInstruction());
  }
}
